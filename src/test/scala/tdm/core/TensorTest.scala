package tdm.core

import org.scalatest._

import shapeless.test.illTyped
import org.apache.spark.sql.SparkSession

import tdm._

class TensorTest extends FlatSpec with Matchers {
	
	implicit val sparkSession = SparkSession.builder().master("local[4]").getOrCreate()
	sparkSession.sparkContext.setLogLevel("ERROR")
	
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
	
	it should "retrieve added value of a Tensor of type Double" in {
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
		tensor(Dimension1.value("d1"), Dimension2.value("d2"), Dimension3.value(1)).get.isInstanceOf[Double] shouldBe true
	}
	
	it should "retrieve added value of a Tensor of type Float" in {
		object Dimension1 extends TensorDimension[String]
		object Dimension2 extends TensorDimension[String]
		object Dimension3 extends TensorDimension[Long]
		
		val tensor = TensorBuilder[Float]
			.addDimension(Dimension1)
			.addDimension(Dimension2)
			.addDimension(Dimension3)
			.build()
		
		tensor.addValue(Dimension1.value("d1"), Dimension2.value("d2"), Dimension3.value(1))(1.0f)
		
		tensor(Dimension1.value("d1"), Dimension2.value("d2"), Dimension3.value(1)) shouldBe Some(1.0f)
		tensor(Dimension1.value("d1"), Dimension2.value("d2"), Dimension3.value(1)).get.isInstanceOf[Float] shouldBe true
	}
	
	it should "retrieve added value of a Tensor of type Long" in {
		object Dimension1 extends TensorDimension[String]
		object Dimension2 extends TensorDimension[String]
		object Dimension3 extends TensorDimension[Long]
		
		val tensor = TensorBuilder[Long]
			.addDimension(Dimension1)
			.addDimension(Dimension2)
			.addDimension(Dimension3)
			.build()
		
		tensor.addValue(Dimension1.value("d1"), Dimension2.value("d2"), Dimension3.value(1))(1L)
		
		tensor(Dimension1.value("d1"), Dimension2.value("d2"), Dimension3.value(1)) shouldBe Some(1L)
		tensor(Dimension1.value("d1"), Dimension2.value("d2"), Dimension3.value(1)).get.isInstanceOf[Long] shouldBe true
	}
	
	it should "retrieve added value of a Tensor of type Int" in {
		object Dimension1 extends TensorDimension[String]
		object Dimension2 extends TensorDimension[String]
		object Dimension3 extends TensorDimension[Long]
		
		val tensor = TensorBuilder[Int]
			.addDimension(Dimension1)
			.addDimension(Dimension2)
			.addDimension(Dimension3)
			.build()
		
		tensor.addValue(Dimension1.value("d1"), Dimension2.value("d2"), Dimension3.value(1))(1)
		
		tensor(Dimension1.value("d1"), Dimension2.value("d2"), Dimension3.value(1)) shouldBe Some(1)
		tensor(Dimension1.value("d1"), Dimension2.value("d2"), Dimension3.value(1)).get.isInstanceOf[Int] shouldBe true
	}
	
	it should "retrieve added value of a Tensor of type Short" in {
		object Dimension1 extends TensorDimension[String]
		object Dimension2 extends TensorDimension[String]
		object Dimension3 extends TensorDimension[Long]
		
		val tensor = TensorBuilder[Short]
			.addDimension(Dimension1)
			.addDimension(Dimension2)
			.addDimension(Dimension3)
			.build()
		
		tensor.addValue(Dimension1.value("d1"), Dimension2.value("d2"), Dimension3.value(1))(1)
		
		tensor(Dimension1.value("d1"), Dimension2.value("d2"), Dimension3.value(1)) shouldBe Some(1)
		tensor(Dimension1.value("d1"), Dimension2.value("d2"), Dimension3.value(1)).get.isInstanceOf[Short] shouldBe true
	}
	
	it should "retrieve added value of a Tensor of type Byte" in {
		object Dimension1 extends TensorDimension[String]
		object Dimension2 extends TensorDimension[String]
		object Dimension3 extends TensorDimension[Long]
		
		val tensor = TensorBuilder[Byte]
			.addDimension(Dimension1)
			.addDimension(Dimension2)
			.addDimension(Dimension3)
			.build()
		
		tensor.addValue(Dimension1.value("d1"), Dimension2.value("d2"), Dimension3.value(1))(1)
		
		tensor(Dimension1.value("d1"), Dimension2.value("d2"), Dimension3.value(1)) shouldBe Some(1)
		tensor(Dimension1.value("d1"), Dimension2.value("d2"), Dimension3.value(1)).get.isInstanceOf[Byte] shouldBe true
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
