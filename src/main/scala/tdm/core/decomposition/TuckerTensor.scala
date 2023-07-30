package tdm.core.decomposition

import mulot.distributed.Tensor
import mulot.distributed.tensordecomposition.tucker.HOOI
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.{DataFrame, SparkSession}
import shapeless.HList
import tdm.core.{TensorDimension, TuckerResultTensor}

private[core] class TuckerTensor[DL <: HList](tensor: Tensor, ranks: Array[Int], typeList: List[TensorDimension[_]])(implicit spark: SparkSession) {
	
	
	private var decomposition: HOOI = HOOI(tensor, ranks)
	
	/**
	 * The maximal number of iterations to perform before stopping the algorithm if the convergence criteria is not met.
	 *
	 * @param maxIterations the number of iterations
	 */
	def withMaxIterations(nbIterations: Int): this.type = {
		decomposition = decomposition.withMaxIterations(nbIterations).asInstanceOf[HOOI]
		this
	}
	
	
	/**
	 * The Frobenius norm is used as convergence criteria to determine when to stop the iteration.
	 * It represents the difference between the core tensors of two iterations.
	 *
	 * @param minFrobenius the threshold of the difference of the Frobenius norm at which stopping the iteration
	 */
	def withMinFrobenius(minFrobenius: Double): this.type = {
		decomposition = decomposition.withMinFrobenius(minFrobenius).asInstanceOf[HOOI]
		this
	}
	
	/**
	 * Choose which method used to initialize factor matrices.
	 *
	 * @param initializer the method to use
	 */
	def withInitializer(initializer: Initializers.Intializer): this.type = {
		initializer match {
			case Initializers.GAUSSIAN => decomposition = decomposition.withInitializer(HOOI.Initializers.gaussian).asInstanceOf[HOOI]
			case Initializers.HOSVD => decomposition = decomposition.withInitializer(HOOI.Initializers.hosvd).asInstanceOf[HOOI]
		}
		this
	}
	
	/**
	 * Run the Tucker decomposition for this tensor, with the given parameters.
	 *
	 * @return A [[TuckerResultTensor]] containing one tensor per dimension, and a core tensor.
	 *         Each [[tdm.core.Tensor]] for the dimensions has 3 dimensions: one with the values of
	 *         the original dimension, one with the values of the rank, and the last one with
	 *         the values found with the Tucker decomposition.
	 */
	def execute(): TuckerResultTensor[DL] = {
		val result = decomposition.execute()
		new TuckerResultTensor[DL](typeList, result, tensor, typeList.zip(ranks).toMap)
	}
}

private[core] object TuckerTensor {
	def apply[DL <: HList](data: DataFrame, ranks: Array[Int], typeList: List[TensorDimension[_]], valueColumnName: String = "val")(implicit spark: SparkSession): TuckerTensor[DL] = {
		new TuckerTensor[DL](Tensor(data.withColumn(valueColumnName, col(valueColumnName).cast("double")),
			valueColumnName), ranks, typeList)
	}
}

