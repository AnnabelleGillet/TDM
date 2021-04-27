package tdm.core

import org.apache.spark.sql.Row
import shapeless.{HList, HMap}
import tdm._

class CollectedTensor[T, DL <: HList] private[core]
		(val typeList: List[TensorDimension[_]], val dimensions: HMap[DimensionMap], val data: Array[Row])
		(implicit tensorTypeAuthorized: Numeric[T]) {
	private var cachedRow: Option[Row] = None
	private var cachedIndex: Option[Int] = None
	
	/**
	 * Number of non-zero elements of this tensor.
	 */
	val size = data.length
	
	/**
	 * Produce the range of all index of the non-zero elements of this tensor.
	 *
	 * @return a [[Range]] value from 0 to one less than the number of non-zero elements of this tensor.
	 */
	def indices: Range = data.indices
	
	/**
	 * Get the value of the tensor at the index i.
	 *
	 * @param i
	 * @return
	 */
	def apply(i: Int): T = {
		val row = getRow(i)
		row.getAs[T](cachedRow.get.fieldIndex(Tensor.TENSOR_VALUES_COLUMN_NAME))
	}
	
	/**
	 * Get the value of the given dimension at the index i.
	 *
	 * @param dimension
	 * @param i
	 * @return
	 */
	def apply[CT, D <: TensorDimension[_]](dimension: D, i: Int)
										  (implicit eq: dimension.DimensionType =:= CT,
										   contains: ContainsConstraint[DL, D]): CT = {
		val row = getRow(i)
		row.getAs[CT](row.fieldIndex(dimension.name))
	}
	
	/**
	 * Sort the values by ascending order.
	 *
	 * @return the sorted [[CollectedTensor]]
	 */
	def orderByValues(): CollectedTensor[T, DL] = {
		new CollectedTensor[T, DL](typeList, dimensions,
			data.sortWith((r1, r2) => tensorTypeAuthorized.lteq(r1.getAs[T](r1.fieldIndex(Tensor.TENSOR_VALUES_COLUMN_NAME)), r2.getAs[T](r2.fieldIndex(Tensor.TENSOR_VALUES_COLUMN_NAME)))))
	}
	
	/**
	 * Sort the values by descending order.
	 *
	 * @return the sorted [[CollectedTensor]]
	 */
	def orderByValuesDesc(): CollectedTensor[T, DL] = {
		new CollectedTensor[T, DL](typeList, dimensions,
			data.sortWith((r1, r2) => tensorTypeAuthorized.gteq(r1.getAs[T](r1.fieldIndex(Tensor.TENSOR_VALUES_COLUMN_NAME)), r2.getAs[T](r2.fieldIndex(Tensor.TENSOR_VALUES_COLUMN_NAME)))))
	}
	
	private def getRow(i: Int): Row = {
		if (cachedIndex.isEmpty || cachedIndex.get != i) {
			cachedRow = Some(data(i))
			cachedIndex = Some(i)
		}
		cachedRow.get
	}
}
