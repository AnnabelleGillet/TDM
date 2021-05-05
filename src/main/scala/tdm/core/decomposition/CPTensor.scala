package tdm.core.decomposition

import mulot.Tensor
import mulot.tensordecomposition.CPALS
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.{DataFrame, SparkSession}
import tdm.core.decomposition.Norm.Norm

private[core] class CPTensor(tensor: Tensor)(implicit spark: SparkSession) {
	/**
	 * Run the CP decomposition for this tensor.
	 *
	 * @param rank the rank of the CP decomposition
	 * @param nbIterations the maximum number of iterations
	 * @param norm the norm to use on the columns of the factor matrices
	 * @param minFms the Factor Match Score limit to stop the algorithm
	 * @param highRank improve the computation of the pinverse if set to true. By default, is true when rank >= 100.
	 * @param computeCorcondia set to true to compute the core consistency diagnostic (CORCONDIA)
	 * @return Two [[Map]], one with the lambdas associated to the corresponding rank, and one
	 *         with the [[String]] name of each dimension of the tensor mapped to a [[DataFrame]].
	 *         This [[DataFrame]] has 3 columns: one with the values of the original dimension, one with the values of the rank,
	 *         and the last one with the value found with the CP.
	 */
	def runCPALS(rank: Int, nbIterations: Int = 25, norm: Norm = Norm.L1,
				 minFms: Double = 0.99, highRank: Option[Boolean] = None, computeCorcondia: Boolean = false): CPResult = {
		val kruskal = CPALS.compute(this.tensor, rank, norm.toString, nbIterations, minFms, highRank, computeCorcondia)
		
		val factorMatrices = (for (i <- tensor.dimensionsName.indices) yield {
			var df = spark.createDataFrame(kruskal.A(i).toCoordinateMatrixWithZeros().entries).toDF("dimIndex", tdm.core.Rank.name, tensor.valueColumnName)
			if (tensor.dimensionsIndex.isDefined) {
				df = df.join(tensor.dimensionsIndex.get(i), "dimIndex").select("dimValue", tdm.core.Rank.name, tensor.valueColumnName)
				df = df.withColumnRenamed("dimValue", tensor.dimensionsName(i))
			} else {
				df = df.withColumnRenamed("dimIndex", tensor.dimensionsName(i))
			}
			tensor.dimensionsName(i) -> df
		}).toMap
		val lambdas = kruskal.lambdas.zipWithIndex.map(l => l._2 -> l._1).toMap
		CPResult(lambdas, factorMatrices, kruskal.corcondia)
	}
	
	case class CPResult(lambda: Map[Int, Double], factorMatrices: Map[String, DataFrame], corcondia: Option[Double])
}

private[core] object CPTensor {
	def apply(data: DataFrame, valueColumnName: String = "val")(implicit spark: SparkSession): CPTensor = {
		new CPTensor(Tensor(data.withColumn(valueColumnName, col(valueColumnName).cast("double")),
			valueColumnName))
	}
}