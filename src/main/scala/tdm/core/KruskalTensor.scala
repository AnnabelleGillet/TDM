package tdm.core

import org.apache.spark.sql.{DataFrame, SparkSession}
import shapeless.{::, HList, HMap, HNil}
import tdm.DimensionMap

class KruskalTensor[DL <: HList] private[core] (val typeList: List[TensorDimension[_]],
												val lambdas: Map[Int, Double],
												val factorMatrices: Map[String, DataFrame],
												val corcondia: Option[Double])
											   (implicit spark: SparkSession) {
	
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
}

object Rank extends TensorDimension[Int]