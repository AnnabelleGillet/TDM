package tdm.core

import mulot.distributed.tensordecomposition.cp.ALS
import org.apache.spark.sql.{DataFrame, SparkSession}
import shapeless.{::, HList, HMap, HNil}
import tdm.DimensionMap

class KruskalTensor[DL <: HList] private[core] (val typeList: List[TensorDimension[_]],
												private val kruskal: ALS#Kruskal,
												private val tensor: mulot.distributed.Tensor)
											   (implicit spark: SparkSession) {
	
	val factorMatrices = (for (i <- tensor.dimensionsName.indices) yield {
		var df = spark.createDataFrame(kruskal.A(i).toCoordinateMatrixWithZeros().entries).toDF("dimIndex", Rank.name, tensor.valueColumnName)
		if (tensor.dimensionsIndex.isDefined) {
			df = df.join(tensor.dimensionsIndex.get(i), "dimIndex").select("dimValue", Rank.name, tensor.valueColumnName)
			df = df.withColumnRenamed("dimValue", tensor.dimensionsName(i))
		} else {
			df = df.withColumnRenamed("dimIndex", tensor.dimensionsName(i))
		}
		tensor.dimensionsName(i) -> df
	}).toMap
	val lambdas = kruskal.lambdas.zipWithIndex.map(l => l._2 -> l._1).toMap
	val corcondia = kruskal.corcondia
	
	/**
	 * Extract a specific dimension result from this [[KruskalTensor]] as a 2-order tensor,
	 * with dimensions [[D]] and [[Rank]].
	 *
	 * @param dimension: the dimension to extract
	 * @return a 2-order tensor with schema [[D]] :: [[Rank]]
	 */
	def extract[D <: TensorDimension[_]](dimension: D): Tensor[Double, D :: Rank.type :: HNil] = {
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
	
	def reconstruct(): Tensor[Double, DL] = ???
	
	object Rank extends TensorDimension[Int]
}
