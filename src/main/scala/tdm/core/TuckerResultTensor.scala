package tdm.core

import mulot.distributed
import mulot.distributed.tensordecomposition.tucker.HOOI
import org.apache.spark.sql.{DataFrame, SparkSession}
import shapeless.{::, HList, HMap, HNil}
import tdm.{ContainsAllDimensionsRankConstraint, DimensionMap}

class TuckerResultTensor[DL <: HList] private[core](val typeList: List[TensorDimension[_]],
													private val result: HOOI#HOOIResult,
													private val tensor: mulot.distributed.Tensor,
													val ranks: Map[TensorDimension[_], Int])
											  (implicit spark: SparkSession) {
	
	val coreTensor: distributed.Tensor = result.coreTensor
	val factorMatrices: Map[String, DataFrame] = (for (i <- tensor.dimensionsName.indices) yield {
		var df = spark.createDataFrame(result.U(i).toCoordinateMatrix().entries).toDF("dimIndex", Rank.name, Tensor.TENSOR_VALUES_COLUMN_NAME)
		if (tensor.dimensionsIndex.isDefined) {
			df = df.join(tensor.dimensionsIndex.get(i), "dimIndex").select("dimValue", Rank.name, Tensor.TENSOR_VALUES_COLUMN_NAME)
			df = df.withColumnRenamed("dimValue", tensor.dimensionsName(i))
		} else {
			df = df.withColumnRenamed("dimIndex", tensor.dimensionsName(i))
		}
		tensor.dimensionsName(i) -> df
	}).toMap
	
	/**
	 * Extract a specific dimension result from this [[TuckerResultTensor]] as a 2-order tensor,
	 * with dimensions [[D]] and [[Rank]].
	 *
	 * @param dimension: the dimension to extract
	 * @return a 2-order tensor with schema [[D]] :: [[Rank]]
	 */
	def extractFactorMatrix[D <: TensorDimension[_]](dimension: D): Tensor[Double, D :: Rank.type :: HNil] = {
		// Initializing the dimensions of the new tensor
		var newDimensions = HMap.empty[DimensionMap]
		newDimensions = newDimensions + (dimension -> dimension.produceDimension())
		newDimensions = newDimensions + (Rank -> Rank.produceDimension())
		
		// Intializing the new tensor
		val tensor = new Tensor[Double, D :: Rank.type :: HNil](typeList, newDimensions)
		tensor.values = factorMatrices(dimension.name)
		tensor.empty = false
		
		tensor
	}
	
	/**
	 * Extract a value of the core tensor for the given rank values of each dimension.
	 *
	 * @param dimensionsValues: all the dimensions of the tensor with the wanted rank
	 * @return the value for the core tensor, or None if the given ranks do not correspond to these of the decomposition.
	 */
	def extractValueOfCoreTensor[P <: Product] (dimensionsValues: P)
							(implicit dimensionsConstraint: ContainsAllDimensionsRankConstraint[DL, P]): Option[Double] = {
		
		var dimensionFound: Boolean = true
		var outRanksRange: Boolean = false
		var currentDataFrame = coreTensor.data
		for (dimensionValue <- dimensionsValues.productIterator if dimensionFound) {
			val (dimension: TensorDimension[_], value) = dimensionValue
			
			currentDataFrame = currentDataFrame.filter(currentDataFrame.col(dimension.name) === value)
			if (currentDataFrame.isEmpty) {
				dimensionFound = false
				if (value.asInstanceOf[Int] >= ranks(dimension) || value.asInstanceOf[Int] < 0) {
					outRanksRange = true
				}
			}
		}
		
		if (dimensionFound && !currentDataFrame.isEmpty) {
			Some(currentDataFrame.select(Tensor.TENSOR_VALUES_COLUMN_NAME).first().get(0).asInstanceOf[Double])
		} else {
			if (outRanksRange) {
				None
			} else {
				Some(0.0)
			}
		}
	}
	
	def reconstruct(): Tensor[Double, DL] = ???
	
	object Rank extends TensorDimension[Int]
}

