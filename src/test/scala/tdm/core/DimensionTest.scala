package test.tdm.core

import org.scalatest._

import shapeless.test.illTyped

import tdm.core.TensorDimension

class DimensionTest extends FlatSpec with Matchers {
	"A TensorDimension" should "compile with Double" in {
		object Dimension extends TensorDimension[Double]
	}
	
	it should "compile with Float" in {
		object Dimension extends TensorDimension[Float]
	}
	
	it should "compile with Long" in {
		object Dimension extends TensorDimension[Long]
	}
	
	it should "compile with Int" in {
		object Dimension extends TensorDimension[Int]
	}
	
	it should "compile with Short" in {
		object Dimension extends TensorDimension[Short]
	}
	
	it should "compile with Byte" in {
		object Dimension extends TensorDimension[Byte]
	}
	
	it should "compile with Boolean" in {
		object Dimension extends TensorDimension[Boolean]
	}
	
	it should "compile with String" in {
		object Dimension extends TensorDimension[String]
	}
    
	it should "not compile with AnyVal" in {
		illTyped("""
		  object Dimension extends TensorDimension[AnyVal]
		  """)
	}
	
	it should "not compile with Any" in {
		illTyped("""
		  object Dimension extends TensorDimension[Any]
		  """)
	}
	
	it should "not compile with AnyRef" in {
		illTyped("""
		  object Dimension extends TensorDimension[AnyRef]
		  """)
	}
	
	it should "not compile with custom trait" in {
		trait InvalidDimension
		illTyped("""
		  object Dimension extends TensorDimension[InvalidDimension]
		  """)
	}
	
	it should "not compile with custom class" in {
		class InvalidDimension {}
		illTyped("""
		  object Dimension extends TensorDimension[InvalidDimension]
		  """)
	}
	
	"A TensorDimension value" should "compile when it is the same type as the TensorDimension" in {
		object Dimension extends TensorDimension[Double]
		Dimension.value(1.0)
	}
	
	it should "not compile when it is not the same type as the TensorDimension" in {
		object Dimension extends TensorDimension[Double]
		illTyped("""
		  Dimension.value("1.0")
		  """)
	}
}