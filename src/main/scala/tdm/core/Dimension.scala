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
	
	private[core] val name: String = this.getClass.getName.replace(".", "_")
	
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
private[tdm] class Dimension[T]