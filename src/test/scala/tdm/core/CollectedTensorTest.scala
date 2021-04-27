package tdm.core

import org.apache.spark.sql.SparkSession
import org.scalatest.{FlatSpec, Matchers}
import shapeless.test.illTyped

class CollectedTensorTest extends FlatSpec with Matchers {
	implicit val sparkSession = SparkSession.builder().master("local[4]").getOrCreate()
	sparkSession.sparkContext.setLogLevel("ERROR")
	
	object Dimension1 extends TensorDimension[String]
	object Dimension2 extends TensorDimension[String]
	
	val tensor = TensorBuilder[Double]
		.addDimension(Dimension1)
		.addDimension(Dimension2)
		.build()
	
	tensor.addValue(Dimension1.value("d11"), Dimension2.value("d21"))(1.0)
	tensor.addValue(Dimension1.value("d11"), Dimension2.value("d22"))(3.0)
	tensor.addValue(Dimension1.value("d12"), Dimension2.value("d21"))(2.0)
	
	val collectedTensor = tensor.collect()
	
	"A CollectedTensor" should "contains the right number of values" in {
		collectedTensor.size shouldBe 3
		collectedTensor.indices shouldBe (0 until 3)
	}
	
	it should "contains the right values" in {
		case class Content(dimension1: String, dimension2: String)
		val rows = Map[Double, Content](1.0 -> Content("d11", "d21"),
			3.0 -> Content("d11", "d22"),
			2.0 -> Content("d12", "d21"))
		var values = List[Double](1.0, 2.0, 3.0)
		
		for (i <- 0 until 3) {
			val content = rows.get(collectedTensor(i)).get
			values.contains(collectedTensor(i)) shouldBe true
			values = values.filter(_ != collectedTensor(i))
			content.dimension1 shouldBe collectedTensor(Dimension1, i)
			content.dimension2 shouldBe collectedTensor(Dimension2, i)
		}
	}
	
	it should "order values in ascending order" in {
		val sortedCollectedTensor = collectedTensor.orderByValues()
		
		sortedCollectedTensor(0) shouldBe 1.0
		sortedCollectedTensor(1) shouldBe 2.0
		sortedCollectedTensor(2) shouldBe 3.0
	}
	
	it should "order values in descending order" in {
		val sortedCollectedTensor = collectedTensor.orderByValuesDesc()
		
		sortedCollectedTensor(0) shouldBe 3.0
		sortedCollectedTensor(1) shouldBe 2.0
		sortedCollectedTensor(2) shouldBe 1.0
	}
	
	it should "not access to a dimension that are not in the tensor" in {
		object Dimension3 extends TensorDimension[String]
		illTyped(
			"""
			collectedTensor(Dimension3, 0)
			""")
	}
}
