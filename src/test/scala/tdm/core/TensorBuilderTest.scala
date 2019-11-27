package test.tdm.core

import org.scalatest._

import shapeless.test.illTyped

import tdm._
import tdm.core._

class TensorBuilderTest extends FlatSpec with Matchers {    
    "A TensorBuilder" should "compile with Double" in {
		TensorBuilder[Double]
	}
	
	it should "compile with Float" in {
		TensorBuilder[Float]
	}
	
	it should "compile with Long" in {
		TensorBuilder[Long]
	}
	
	it should "compile with Int" in {
		TensorBuilder[Int]
	}
	
	it should "compile with Short" in {
		TensorBuilder[Short]
	}
	
	it should "compile with Byte" in {
		TensorBuilder[Byte]
	}
	
	it should "compile with Boolean" in {
		TensorBuilder[Boolean]
	}
	
	it should "compile with Char" in {
		TensorBuilder[Char]
	}
	
	it should "compile with String" in {
		TensorBuilder[String]
	}
    
	it should "not compile with AnyVal" in {
		illTyped("""
		  TensorBuilder[AnyVal]
		  """)
	}
	
	it should "not compile with Any" in {
		illTyped("""
		  TensorBuilder[Any]
		  """)
	}
	
	it should "not compile with AnyRef" in {
		illTyped("""
		  TensorBuilder[AnyRef]
		  """)
	}
	
	it should "not compile with custom object" in {
		object InvalidDimension
		illTyped("""
		  TensorBuilder[InvalidDimension]
		  """)
	}
	
	it should "not compile with custom class" in {
		class InvalidDimension {}
		illTyped("""
		  TensorBuilder[InvalidDimension]
		  """)
	}
	
	it should "accept object that extends TensorDimension" in {
		object Dimension1 extends TensorDimension[String]
    	object Dimension2 extends TensorDimension[String]
		
		val tensor = TensorBuilder[Double]
	        .addDimension(Dimension1)
	        .addDimension(Dimension2)
	}
	
	it should "not accept object that does not extend TensorDimension" in {
		object Dimension1
    	object Dimension2
    	object Dimension3 extends TensorDimension[String]
		
    	illTyped("""
    	  val tensor = TensorBuilder[Double]
	        .addDimension(Dimension1)
	        .addDimension(Dimension2)
    	  """)
		
		illTyped("""
    	  val tensor = TensorBuilder[Double]
	        .addDimension(Dimension3)
	        .addDimension(Dimension2)
    	  """)
	}
	
	it should "not accept twice the same dimension" in {
		object Dimension1 extends TensorDimension[String]
    	object Dimension2 extends TensorDimension[String]
		
    	illTyped("""
    	  val tensor = TensorBuilder[Double]
	        .addDimension(Dimension1)
	        .addDimension(Dimension1)
    	  """)
		
		illTyped("""
    	  val tensor = TensorBuilder[Double]
	        .addDimension(Dimension1)
	        .addDimension(Dimension2)
	        .addDimension(Dimension1)
    	  """)
    	  
    	  illTyped("""
    	  val tensor = TensorBuilder[Double]
	        .addDimension(Dimension1)
	        .addDimension(Dimension2)
	        .addDimension(Dimension2)
    	  """)
	}
	
	it should "build tensor" in {
		object Dimension1 extends TensorDimension[String]
    	object Dimension2 extends TensorDimension[String]
    	object Dimension3 extends TensorDimension[Long]
		
		val tensor = TensorBuilder[Double]
	        .addDimension(Dimension1)
	        .addDimension(Dimension2)
	        .addDimension(Dimension3)
	        .build()
	}
	
	it should "not build tensor with 0 dimension" in {
		illTyped("""
		  val tensor = TensorBuilder[Double].build()
		  """)
	}
}