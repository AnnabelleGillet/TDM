package tdm.core.decomposition

import mulot.distributed.Tensor
import mulot.distributed.tensordecomposition.cp.ALS
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.{DataFrame, SparkSession}
import shapeless.HList
import tdm.core.{KruskalTensor, TensorDimension}
import tdm.core.decomposition.Norm.Norm

private[core] class CPTensor[DL <: HList](tensor: Tensor, rank: Int, typeList: List[TensorDimension[_]])(implicit spark: SparkSession) {

	private var decomposition: ALS = ALS(tensor, rank)
	
	/**
	 * The maximal number of iterations to perform before stopping the algorithm if the convergence criteria is not met.
	 *
	 * @param maxIterations the number of iterations
	 */
	def withMaxIterations(nbIterations: Int): this.type = {
		decomposition = decomposition.withMaxIterations(nbIterations).asInstanceOf[ALS]
		this
	}
	
	/**
	 * The norm to use to normalize the factor matrices.
	 *
	 * @param norm the chosen norm
	 */
	def withNorm(norm: Norm): this.type = {
		decomposition = decomposition.withNorm(norm).asInstanceOf[ALS]
		this
	}
	
	/**
	 * The Factor Match Score (FMS) is used as convergence criteria to determine when to stop the iteration.
	 * It represents the similarity between the factor matrices of two iterations, with a value between 0 and 1 (at 0
	 * the matrices are completely different, and they are the same at 1).
	 *
	 * @param minFms the threshold of the FMS at which stopping the iteration
	 */
	def withMinFms(minFms: Double): this.type = {
		decomposition = decomposition.withMinFms(minFms).asInstanceOf[ALS]
		this
	}
	
	/**
	 * If the rank of the decomposition is high, allows to optimize it
	 * in order to accelerate the execution.
	 *
	 * @param highRank true to optimize the algorithm with high rank, false otherwise
	 */
	def withHighRank(highRank: Boolean): this.type = {
		decomposition = decomposition.withHighRank(highRank).asInstanceOf[ALS]
		this
	}
	
	/**
	 * Choose if CORCONDIA must be computed after the execution of the decomposition.
	 *
	 * @param computeCorcondia true to compute CORCONDIA, false otherwise
	 */
	def withComputeCorcondia(computeCorcondia: Boolean): this.type = {
		decomposition = decomposition.withComputeCorcondia(computeCorcondia).asInstanceOf[ALS]
		this
	}
	
	/**
	 * Choose which method used to initialize factor matrices.
	 *
	 * @param initializer the method to use
	 */
	def withInitializer(initializer: Initializers.Intializer): this.type = {
		initializer match {
			case Initializers.GAUSSIAN => decomposition = decomposition.withInitializer(ALS.Initializers.gaussian).asInstanceOf[ALS]
			case Initializers.HOSVD => decomposition = decomposition.withInitializer(ALS.Initializers.hosvd).asInstanceOf[ALS]
		}
		this
	}
	
	/**
	 * Run the CP decomposition for this tensor, with the given parameters.
	 *
	 * @return A [[KruskalTensor]] containing one tensor per dimension.
	 *         Each [[tdm.core.Tensor]] has 3 dimensions: one with the values of the original dimension,
	 *         one with the values of the rank, and the last one with the values found with the CP.
	 */
	def execute(): KruskalTensor[DL] = {
		val kruskal = decomposition.execute()
		new KruskalTensor[DL](typeList, kruskal, tensor)
	}
}

private[core] object CPTensor {
	def apply[DL <: HList](data: DataFrame, rank: Int, typeList: List[TensorDimension[_]], valueColumnName: String = "val")(implicit spark: SparkSession): CPTensor[DL] = {
		new CPTensor[DL](Tensor(data.withColumn(valueColumnName, col(valueColumnName).cast("double")),
			valueColumnName), rank, typeList)
	}
}