package test.tdm.core

import org.scalatest._

import shapeless.test.illTyped

import tdm._
import tdm.core._

class MainTest extends FlatSpec with Matchers {
    "Main" should "test" in {
    	
    	object User extends TensorDimension[String]
    	object User1 extends TensorDimension[String]
    	object Hashtag extends TensorDimension[String]
    	object Time extends TensorDimension[Long]
        
	    val tensor = TensorBuilder[Double]
	        .addDimension(User)
	        .addDimension(Hashtag)
	        .addDimension(Time)
	        .build()
	    
	    val tensorTest = TensorBuilder[Double]
            .addDimension(User1)
            .build()
            
	    val tensorTest2 = TensorBuilder[Double]
            .addDimension(User1)
            .addDimension(User)
            .build()
            
	    val r1 = tensor.naturalJoin(tensor)
	    tensor.naturalJoin(tensorTest2)
	    illTyped("""
	    tensor.naturalJoin(tensorTest)
	    """)
	        
	    val tensorBuilder = TensorBuilder[Double]
	        .addDimension(User)
	        .addDimension(Hashtag)
	        
	    val t1 = tensorBuilder.build()
	    val t2 = tensorBuilder.build()
	    
	    t1.addValue(User.value("u1"), Hashtag.value("ht1"))(3.0)
	    t2.addValue(User.value("u2"), Hashtag.value("ht1"))(2.0)
	    t2.addValue(User.value("u1"), Hashtag.value("ht1"))(4.0)
	    
	    val t3 = t1.union(t2)((v1, v2) => v1 + v2)
	    //info(t1(User.value("u1"), Hashtag.value("ht1")).toString)
	    //info(t2(User.value("u1"), Hashtag.value("ht1")).toString)
	    info("union")
	    info(t3(User.value("u1"), Hashtag.value("ht1")).toString)
	    info(t3(User.value("u2"), Hashtag.value("ht1")).toString)
	    
	    val t4 = t1.intersection(t2)((v1, v2) => v1 + v2)
	    info("intersection")
	    info(t4(User.value("u1"), Hashtag.value("ht1")).toString)
	    info(t4(User.value("u2"), Hashtag.value("ht1")).toString)
	    
	    tensor.addValue(User.value("u1"), Hashtag.value("ht1"), Time.value(1))(1.0)
	    tensor.addValue(User.value("u2"), Hashtag.value("ht2"), Time.value(2))(2.0)

	    /*info(tensor(User.value("u1"), Hashtag.value("ht1"), Time.value(1)).toString) // Some(1.0)
	    info(tensor(User.value("u2"), Hashtag.value("ht2"), Time.value(2)).toString) // Some(2.0)
	    info(tensor(User.value("u2"), Hashtag.value("ht1"), Time.value(1)).toString) // None*/
	    
	    //tensor.union(t1)
	    //tensor.union(tensor)
	    
	    val tensor2 = tensor.projection(User)("u1")
	    info("projection")
	    info(tensor2(Hashtag.value("ht1"), Time.value(1)).toString) // Some(1.0)
	    info(tensor2(Hashtag.value("ht2"), Time.value(2)).toString) // None
	    
	    import Tensor._
	    //import Tensor.valueToTuple1
	    val tensor3 = π(tensor)(Hashtag)("ht2")	    
	    info(tensor3(User.value("u1"), Time.value(1)).toString) // None
	    info(tensor3(User.value("u2"), Time.value(2)).toString) // Some(2.0)

	    val tensor4 = tensor.restriction(Tuple1(Time.condition(v => v == 1)))
	    info("restriction")
	    info(tensor4(User.value("u1"), Hashtag.value("ht1"), Time.value(1)).toString) // Some(1.0)
	    info(tensor4(User.value("u2"), Hashtag.value("ht2"), Time.value(2)).toString) // None
	    info(tensor4(User.value("u2"), Hashtag.value("ht1"), Time.value(1)).toString) // None
	    
	    val tensor5 = tensor.selection(_ > 1)
	    info("sélection")
	    info(tensor5(User.value("u1"), Hashtag.value("ht1"), Time.value(1)).toString) // None
	    info(tensor5(User.value("u2"), Hashtag.value("ht2"), Time.value(2)).toString) // Some(2.0)
	    info(tensor5(User.value("u2"), Hashtag.value("ht1"), Time.value(1)).toString) // None
    }
}