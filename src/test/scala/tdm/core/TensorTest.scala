package test.tdm.core

import org.scalatest._

import shapeless.test.illTyped

import tdm._
import tdm.core._

class TensorTest extends FlatSpec with Matchers {
    "A Tensor" should "accept new value" in {
    	object Dimension1 extends TensorDimension[String]
    	object Dimension2 extends TensorDimension[String]
    	object Dimension3 extends TensorDimension[Long]
		
		val tensor = TensorBuilder[Double]
	        .addDimension(Dimension1)
	        .addDimension(Dimension2)
	        .addDimension(Dimension3)
	        .build()
	        
	    tensor.addValue(Dimension1.value("d1"), Dimension2.value("d2"), Dimension3.value(1))(1.0)
    }
    
    it should "retrieve added value" in {
    	object Dimension1 extends TensorDimension[String]
    	object Dimension2 extends TensorDimension[String]
    	object Dimension3 extends TensorDimension[Long]
		
		val tensor = TensorBuilder[Double]
	        .addDimension(Dimension1)
	        .addDimension(Dimension2)
	        .addDimension(Dimension3)
	        .build()
	        
	    tensor.addValue(Dimension1.value("d1"), Dimension2.value("d2"), Dimension3.value(1))(1.0)
	    
	    tensor(Dimension1.value("d1"), Dimension2.value("d2"), Dimension3.value(1)) shouldBe Some(1.0)
    }
    
    it should "retrieve added value in different dimension order" in {
    	object Dimension1 extends TensorDimension[String]
    	object Dimension2 extends TensorDimension[String]
    	object Dimension3 extends TensorDimension[Long]
		
		val tensor = TensorBuilder[Double]
	        .addDimension(Dimension1)
	        .addDimension(Dimension2)
	        .addDimension(Dimension3)
	        .build()
	        
	    tensor.addValue(Dimension1.value("d1"), Dimension2.value("d2"), Dimension3.value(1))(1.0)
	    
	    tensor(Dimension2.value("d2"), Dimension3.value(1), Dimension1.value("d1")) shouldBe Some(1.0)
    }
    
    it should "not retrieve value not added" in {
    	object Dimension1 extends TensorDimension[String]
    	object Dimension2 extends TensorDimension[String]
    	object Dimension3 extends TensorDimension[Long]
		
		val tensor = TensorBuilder[Double]
	        .addDimension(Dimension1)
	        .addDimension(Dimension2)
	        .addDimension(Dimension3)
	        .build()
	        
	    tensor.addValue(Dimension1.value("d1"), Dimension2.value("d2"), Dimension3.value(1))(1.0)
	    
    	tensor(Dimension1.value("d1"), Dimension2.value("d2"), Dimension3.value(2)) shouldBe None
    }
    
    it should "not accept new value of wrong type" in {
    	object Dimension1 extends TensorDimension[String]
    	object Dimension2 extends TensorDimension[String]
    	object Dimension3 extends TensorDimension[Long]
		
		val tensor = TensorBuilder[Double]
	        .addDimension(Dimension1)
	        .addDimension(Dimension2)
	        .addDimension(Dimension3)
	        .build()
	    
	    illTyped("""
	      tensor.addValue(Dimension1.value("d1"), Dimension2.value("d2"), Dimension3.value(1))("1")
	      """)
    }
    
    it should "accept new value from any order of dimensions" in {
    	object Dimension1 extends TensorDimension[String]
    	object Dimension2 extends TensorDimension[String]
    	object Dimension3 extends TensorDimension[Long]
		
		val tensor = TensorBuilder[Double]
	        .addDimension(Dimension1)
	        .addDimension(Dimension2)
	        .addDimension(Dimension3)
	        .build()
	    
	    tensor.addValue(Dimension2.value("d21"), Dimension1.value("d11"), Dimension3.value(1))(1.0)
	    tensor.addValue(Dimension1.value("d12"), Dimension2.value("d22"), Dimension3.value(1))(2.0)
	    tensor.addValue(Dimension3.value(1), Dimension1.value("d13"), Dimension2.value("d23"))(3.0)
	    
	    tensor(Dimension1.value("d11"), Dimension2.value("d21"), Dimension3.value(1)) shouldBe Some(1.0)
    	tensor(Dimension1.value("d12"), Dimension2.value("d22"), Dimension3.value(1)) shouldBe Some(2.0)
    	tensor(Dimension1.value("d13"), Dimension2.value("d23"), Dimension3.value(1)) shouldBe Some(3.0)
    }
    
    it should "not accept new value from wrong number of dimensions" in {
    	object Dimension1 extends TensorDimension[String]
    	object Dimension2 extends TensorDimension[String]
    	object Dimension3 extends TensorDimension[Long]
		
		val tensor = TensorBuilder[Double]
	        .addDimension(Dimension1)
	        .addDimension(Dimension2)
	        .addDimension(Dimension3)
	        .build()
	    
	    illTyped("""
	      tensor.addValue(Dimension2.value("d1"), Dimension1.value("d2"))(1.0)
	      """)
    }
}