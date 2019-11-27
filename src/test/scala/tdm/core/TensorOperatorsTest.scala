package test.tdm.core

import org.scalatest._

import shapeless.test.illTyped

import tdm._
import tdm.core._

import Tensor._

class TensorOperatorsTest extends FlatSpec with Matchers {    
    object Dimension1 extends TensorDimension[String]
	object Dimension2 extends TensorDimension[String]
	object Dimension3 extends TensorDimension[Long]
	
	val tensor = TensorBuilder[Double]
        .addDimension(Dimension1)
        .addDimension(Dimension2)
        .addDimension(Dimension3)
        .build()
    /**
     * Projection
     */
    "A Tensor projection" should "accept dimension of the tensor" in {   
	    tensor.projection(Dimension1)("d1")
	    π(tensor)(Dimension1)("d1")
    }
    
    it should "keep the right values in the new tensor" in {
    	val tensor = TensorBuilder[Double]
            .addDimension(Dimension1)
            .addDimension(Dimension2)
            .addDimension(Dimension3)
            .build()
        
        tensor.addValue(Dimension1.value("d1"), Dimension2.value("d2"), Dimension3.value(1))(1.0)
        tensor.addValue(Dimension1.value("d12"), Dimension2.value("d22"), Dimension3.value(1))(2.0)
        
        val tensorProjection1 = tensor.projection(Dimension1)("d1")
        
        tensorProjection1(Dimension2.value("d2"), Dimension3.value(1)) shouldBe Some(1.0)
    	tensorProjection1(Dimension2.value("d22"), Dimension3.value(1)) shouldBe None
    	
    	val tensorProjection2 = π(tensor)(Dimension1)("d1")
        
        tensorProjection2(Dimension2.value("d2"), Dimension3.value(1)) shouldBe Some(1.0)
    	tensorProjection2(Dimension2.value("d22"), Dimension3.value(1)) shouldBe None
    }
    
    it should "not accept dimension value with the wrong type" in {
	    illTyped("""
	      tensor.projection(Dimension1)(1)
	      """)
	    illTyped("""
	      π(tensor)(Dimension1)(1)
	      """) 
    }
    
    it should "not accept dimension that are not part of the tensor" in {
    	object Dimension4 extends TensorDimension[String]
	    illTyped("""
	      tensor.projection(Dimension4)("d4")
	      """)
	    illTyped("""
	      π(tensor)(Dimension4)("d4")
	      """) 
    }
    
    /**
     * Union
     */
    "A Tensor union" should "accept tensor of same schema" in {   
	    tensor.union(tensor)((v1, v2) => v1)
	    tensor.∪(tensor)((v1, v2) => v1)
    }
    
    it should "not accept tensor of different schema" in {
    	val tensor2 = TensorBuilder[Double]
    	    .addDimension(Dimension1)
    	    .addDimension(Dimension2)
    	    .build()
    	illTyped("""
	        tensor.union(tensor2)((v1, v2) => v1)
	    """)
	    illTyped("""
	        tensor.∪(tensor2)((v1, v2) => v1)
	    """)
    }
    
    it should "perform union on 2 tensors" in {
    	val tensor = TensorBuilder[Double]
            .addDimension(Dimension1)
            .addDimension(Dimension2)
            .addDimension(Dimension3)
            .build()
        
        tensor.addValue(Dimension1.value("d11"), Dimension2.value("d21"), Dimension3.value(1))(1.0)
        tensor.addValue(Dimension1.value("d12"), Dimension2.value("d22"), Dimension3.value(2))(2.0)
        
    	val tensor2 = TensorBuilder[Double]
    	    .addDimension(Dimension1)
    	    .addDimension(Dimension2)
    	    .addDimension(Dimension3)
    	    .build()
    	    
    	tensor2.addValue(Dimension1.value("d12"), Dimension2.value("d22"), Dimension3.value(2))(5.0)    
    	tensor2.addValue(Dimension1.value("d13"), Dimension2.value("d23"), Dimension3.value(3))(3.0)
        tensor2.addValue(Dimension1.value("d14"), Dimension2.value("d24"), Dimension3.value(4))(4.0)
	    
        val tensorUnion = tensor.union(tensor2)((v1, v2) => v1 + v2)
        
        tensorUnion(Dimension1.value("d11"), Dimension2.value("d21"), Dimension3.value(1)) shouldBe Some(1.0)
    	tensorUnion(Dimension1.value("d12"), Dimension2.value("d22"), Dimension3.value(2)) shouldBe Some(7.0)
    	tensorUnion(Dimension1.value("d13"), Dimension2.value("d23"), Dimension3.value(3)) shouldBe Some(3.0)
    	tensorUnion(Dimension1.value("d14"), Dimension2.value("d24"), Dimension3.value(4)) shouldBe Some(4.0)
    }
    
    /**
     * Intersection
     */
    "A Tensor intersection" should "accept tensor of same schema" in {   
	    tensor.intersection(tensor)((v1, v2) => v1)
	    tensor.∩(tensor)((v1, v2) => v1)
    }
    
    it should "not accept tensor of different schema" in {
    	val tensor2 = TensorBuilder[Double]
    	    .addDimension(Dimension1)
    	    .addDimension(Dimension2)
    	    .build()
    	illTyped("""
	        tensor.intersection(tensor2)((v1, v2) => v1)
	    """)
	    illTyped("""
	        tensor.∩(tensor2)((v1, v2) => v1)
	    """)
    }
    
    it should "perform union on 2 tensors" in {
    	val tensor = TensorBuilder[Double]
            .addDimension(Dimension1)
            .addDimension(Dimension2)
            .addDimension(Dimension3)
            .build()
        
        tensor.addValue(Dimension1.value("d11"), Dimension2.value("d21"), Dimension3.value(1))(1.0)
        tensor.addValue(Dimension1.value("d12"), Dimension2.value("d22"), Dimension3.value(2))(2.0)
        
    	val tensor2 = TensorBuilder[Double]
    	    .addDimension(Dimension1)
    	    .addDimension(Dimension2)
    	    .addDimension(Dimension3)
    	    .build()
    	    
    	tensor2.addValue(Dimension1.value("d12"), Dimension2.value("d22"), Dimension3.value(2))(5.0)    
    	tensor2.addValue(Dimension1.value("d13"), Dimension2.value("d23"), Dimension3.value(3))(3.0)
        tensor2.addValue(Dimension1.value("d14"), Dimension2.value("d24"), Dimension3.value(4))(4.0)
	    
        val tensorIntersection = tensor.intersection(tensor2)((v1, v2) => v1 + v2)
        
        tensorIntersection(Dimension1.value("d11"), Dimension2.value("d21"), Dimension3.value(1)) shouldBe None
    	tensorIntersection(Dimension1.value("d12"), Dimension2.value("d22"), Dimension3.value(2)) shouldBe Some(7.0)
    	tensorIntersection(Dimension1.value("d13"), Dimension2.value("d23"), Dimension3.value(3)) shouldBe None
    	tensorIntersection(Dimension1.value("d14"), Dimension2.value("d24"), Dimension3.value(4)) shouldBe None
    }
    
    /**
     * Restriction
     */
    "A Tensor restriction" should "accept dimension's condition of the tensor" in {   
	    tensor.restriction(Tuple1(Dimension1.condition(v => true)))
    }
    
    it should "accept dimension's condition of dimension not in the tensor" in {
    	object Dimension4 extends TensorDimension[String]

    	illTyped("""
	        tensor.restriction(Tuple1(Dimension4.condition(v => true)))
	    """)
    }
    
    it should "perform restriction on a tensor" in {    	
    	val tensor = TensorBuilder[Double]
            .addDimension(Dimension1)
            .addDimension(Dimension2)
            .addDimension(Dimension3)
            .build()
        
        tensor.addValue(Dimension1.value("d11"), Dimension2.value("d21"), Dimension3.value(1))(4.0)
        tensor.addValue(Dimension1.value("d12"), Dimension2.value("d22"), Dimension3.value(2))(3.0)   
    	tensor.addValue(Dimension1.value("d13"), Dimension2.value("d23"), Dimension3.value(3))(2.0)
        tensor.addValue(Dimension1.value("d14"), Dimension2.value("d24"), Dimension3.value(4))(1.0)
	    
        val tensorRestriction = tensor.restriction(Tuple1(Dimension3.condition(v => v > 2)))
        
        tensorRestriction(Dimension1.value("d11"), Dimension2.value("d21"), Dimension3.value(1)) shouldBe None
    	tensorRestriction(Dimension1.value("d12"), Dimension2.value("d22"), Dimension3.value(2)) shouldBe None
    	tensorRestriction(Dimension1.value("d13"), Dimension2.value("d23"), Dimension3.value(3)) shouldBe Some(2.0)
    	tensorRestriction(Dimension1.value("d14"), Dimension2.value("d24"), Dimension3.value(4)) shouldBe Some(1.0)
    }
    
    /**
     * Selection
     */
    "A Tensor selection" should "accept condition of the tensor type" in {   
	    tensor.selection(v => v > 1.0)
    }
    
    it should "not accept condition with type different than the tensor type" in {
    	illTyped("""
	        tensor.selection((v: String) => v == "")
	    """)
    }
    
    it should "perform selection on a tensor" in {    	
    	val tensor = TensorBuilder[Double]
            .addDimension(Dimension1)
            .addDimension(Dimension2)
            .addDimension(Dimension3)
            .build()
        
        tensor.addValue(Dimension1.value("d11"), Dimension2.value("d21"), Dimension3.value(1))(4.0)
        tensor.addValue(Dimension1.value("d12"), Dimension2.value("d22"), Dimension3.value(2))(3.0)   
    	tensor.addValue(Dimension1.value("d13"), Dimension2.value("d23"), Dimension3.value(3))(2.0)
        tensor.addValue(Dimension1.value("d14"), Dimension2.value("d24"), Dimension3.value(4))(1.0)
	    
        val tensorSelection = tensor.selection(v => v > 2)
        
        tensorSelection(Dimension1.value("d11"), Dimension2.value("d21"), Dimension3.value(1)) shouldBe Some(4.0)
    	tensorSelection(Dimension1.value("d12"), Dimension2.value("d22"), Dimension3.value(2)) shouldBe Some(3.0)
    	tensorSelection(Dimension1.value("d13"), Dimension2.value("d23"), Dimension3.value(3)) shouldBe None
    	tensorSelection(Dimension1.value("d14"), Dimension2.value("d24"), Dimension3.value(4)) shouldBe None
    }
    
    /**
     * Natural join
     */
    "A Tensor natural join" should "accept tensor with common dimensions" in {   
	    object Dimension4 extends TensorDimension[String]
    	
    	val tensor2 = TensorBuilder[Double]
            .addDimension(Dimension1)
            .addDimension(Dimension2)
            .addDimension(Dimension4)
            .build()
        
        tensor.naturalJoin(tensor2)
    }
    
    it should "not accept tensor with no common dimension" in {
    	object Dimension4 extends TensorDimension[String]
    	object Dimension5 extends TensorDimension[String]
    	object Dimension6 extends TensorDimension[Long]
    	
    	val tensor2 = TensorBuilder[Double]
            .addDimension(Dimension4)
            .addDimension(Dimension5)
            .addDimension(Dimension6)
            .build()
        
        illTyped("""
            tensor.naturalJoin(tensor2)
        """)
    }
    
    it should "join 2 tensors" in {   	
    	val tensor = TensorBuilder[Double]
            .addDimension(Dimension1)
            .addDimension(Dimension2)
            .build()
        
        tensor.addValue(Dimension1.value("d11"), Dimension2.value("d21"))(1.0)
        tensor.addValue(Dimension1.value("d12"), Dimension2.value("d22"))(2.0)
        tensor.addValue(Dimension1.value("d11"), Dimension2.value("d22"))(3.0)
        tensor.addValue(Dimension1.value("d11"), Dimension2.value("d23"))(4.0)
        
        val tensor2 = TensorBuilder[Double]
            .addDimension(Dimension2)
            .addDimension(Dimension3)
            .build()
        tensor2.addValue(Dimension2.value("d21"), Dimension3.value(1))(5.0)
        tensor2.addValue(Dimension2.value("d21"), Dimension3.value(2))(6.0)
        tensor2.addValue(Dimension2.value("d22"), Dimension3.value(3))(7.0)
        tensor2.addValue(Dimension2.value("d22"), Dimension3.value(4))(8.0)
        tensor2.addValue(Dimension2.value("d24"), Dimension3.value(5))(9.0)
        
        val tensorJoin = tensor.naturalJoin(tensor2)
        
        tensorJoin(Dimension1.value("d11"), Dimension2.value("d21"), Dimension3.value(1)) shouldBe Some(1.0)
    	tensorJoin(Dimension1.value("d11"), Dimension2.value("d21"), Dimension3.value(2)) shouldBe Some(1.0)
    	tensorJoin(Dimension1.value("d12"), Dimension2.value("d22"), Dimension3.value(3)) shouldBe Some(2.0)
    	tensorJoin(Dimension1.value("d12"), Dimension2.value("d22"), Dimension3.value(4)) shouldBe Some(2.0)
    	tensorJoin(Dimension1.value("d11"), Dimension2.value("d22"), Dimension3.value(3)) shouldBe Some(3.0)
    	tensorJoin(Dimension1.value("d11"), Dimension2.value("d22"), Dimension3.value(4)) shouldBe Some(3.0)
    	tensorJoin(Dimension1.value("d11"), Dimension2.value("d23"), Dimension3.value(1)) shouldBe None
    	tensorJoin(Dimension1.value("d11"), Dimension2.value("d24"), Dimension3.value(5)) shouldBe None
    }
}