package tdm.core

import tdm._

/**
 * Class to extend in order to create a tensor dimension.
 * 
 * For example:
 * object Dimension1 extends TensorDimension[String]
 * 
 * Only types Double, Float, Long, Int, Short, Byte, Boolean, Char or String are authorized.
 */
abstract class TensorDimension[T](implicit typeAuthorized: AuthorizedType[T]) {
	
	type DimensionType = T
	
	/**
	 * Creates a value for this dimension.
	 */
	def value(value: T): (this.type, T) = {
		(this, value)
	}
	
	/**
	 * Creates a condition for this dimension.
	 */
	def condition(condition: T => Boolean): (this.type, T => Boolean) = {
		(this, condition)
	}
	
	/**
	 * Produces a new instance of Dimension[T], corresponding to the internal implementation of a TensorDimension[T].
	 */
	private[core] def produceDimension(): Dimension[T] = {
		new Dimension[T]()
	}
}

/**
 * Internal representation of a tensor dimension.
 */
private[tdm] class Dimension[T] {
	private val values = scala.collection.mutable.Map[T, Long]()
	private val reverseValues = scala.collection.mutable.Map[Long, T]()
	
	private val valueToTensorKey = scala.collection.mutable.Map[Long, String]()
	private var lastIndice: Long = 0
	
	/**
	 * Add a value to this dimension, and return the corresponding index. If the value already exists, return the previous index.
	 * 
	 * @param value: The value to add
	 * 
	 * @return the index of the added value.
	 */
	def addValue(value: T): Long = {
		if (!values.contains(value)) {
    		values.put(value, lastIndice)
    		reverseValues.put(lastIndice, value)
    		lastIndice += 1
		}
		values.get(value).get
	}
	
	/**
	 * Get the indice for the given value.
	 * 
	 * @param value: The value for which to retrieve the index.
	 * 
	 * @return Some(Long) corresponding to the value if it exists, None otherwise.
	 */
	def getIndex(value: T): Option[Long] = {
		values.get(value)
	}
	
	/**
	 * Get the value for the given index.
	 * 
	 * @param index: The value for which to retrieve the index.
	 * 
	 * @return Some(Long) corresponding to the value if it exists, None otherwise.
	 */
	def getValue(index: Long): Option[T] = {
		reverseValues.get(index)
	}
}