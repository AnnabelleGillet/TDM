package tdm.core

import org.apache.spark.sql.SparkSession
import org.scalatest.{FlatSpec, Matchers}

class CPDecompositionTest extends FlatSpec with Matchers {
	object Dimension1 extends TensorDimension[String]
	object Dimension2 extends TensorDimension[String]
	object Dimension3 extends TensorDimension[Long]
	
	implicit val sparkSession = SparkSession.builder().master("local[4]").getOrCreate()
	sparkSession.sparkContext.setLogLevel("ERROR")
	
	val tensor = TensorBuilder[Int]
		.addDimension(Dimension1)
		.addDimension(Dimension2)
		.addDimension(Dimension3)
		.build()
	
	tensor.addValue(Dimension1.value("d11"), Dimension2.value("d21"), Dimension3.value(1))(1)
	tensor.addValue(Dimension1.value("d11"), Dimension2.value("d22"), Dimension3.value(1))(15)
	tensor.addValue(Dimension1.value("d11"), Dimension2.value("d21"), Dimension3.value(2))(3)
	tensor.addValue(Dimension1.value("d12"), Dimension2.value("d22"), Dimension3.value(1))(5)
	tensor.addValue(Dimension1.value("d12"), Dimension2.value("d21"), Dimension3.value(1))(2)
	tensor.addValue(Dimension1.value("d12"), Dimension2.value("d22"), Dimension3.value(2))(4)
	
	"A Tensor" should "run the CP decomposition" in {
		tensor.canonicalPolyadicDecomposition(3, 1)
	}
	
	val kruskal = tensor.canonicalPolyadicDecomposition(3, 1)
	
	"The Kruskal Tensor" should "be valid" in {
		kruskal.lambdas.size shouldBe 3
		kruskal.factorMatrices.size shouldBe 3
	}
	
	it should "contain values for the original dimension 1" in {
		val kruskalDimension1 = kruskal.extract(Dimension1)
		
		kruskalDimension1.count() shouldBe 3 * 2
		kruskalDimension1(Dimension1.value("d11"), Rank.value(0)).isDefined shouldBe true
		kruskalDimension1(Dimension1.value("d11"), Rank.value(1)).isDefined shouldBe true
		kruskalDimension1(Dimension1.value("d11"), Rank.value(2)).isDefined shouldBe true
		kruskalDimension1(Dimension1.value("d12"), Rank.value(0)).isDefined shouldBe true
		kruskalDimension1(Dimension1.value("d12"), Rank.value(1)).isDefined shouldBe true
		kruskalDimension1(Dimension1.value("d12"), Rank.value(2)).isDefined shouldBe true
	}
	
	it should "contain values for the original dimension 2" in {
		val kruskalDimension2 = kruskal.extract(Dimension2)
		
		kruskalDimension2.count() shouldBe 3 * 2
		kruskalDimension2(Dimension2.value("d21"), Rank.value(0)).isDefined shouldBe true
		kruskalDimension2(Dimension2.value("d21"), Rank.value(1)).isDefined shouldBe true
		kruskalDimension2(Dimension2.value("d21"), Rank.value(2)).isDefined shouldBe true
		kruskalDimension2(Dimension2.value("d22"), Rank.value(0)).isDefined shouldBe true
		kruskalDimension2(Dimension2.value("d22"), Rank.value(1)).isDefined shouldBe true
		kruskalDimension2(Dimension2.value("d22"), Rank.value(2)).isDefined shouldBe true
	}
	
	it should "contain values for the original dimension 3" in {
		val kruskalDimension3 = kruskal.extract(Dimension3)
		
		kruskalDimension3.count() shouldBe 3 * 2
		kruskalDimension3(Dimension3.value(1), Rank.value(0)).isDefined shouldBe true
		kruskalDimension3(Dimension3.value(1), Rank.value(1)).isDefined shouldBe true
		kruskalDimension3(Dimension3.value(1), Rank.value(2)).isDefined shouldBe true
		kruskalDimension3(Dimension3.value(2), Rank.value(0)).isDefined shouldBe true
		kruskalDimension3(Dimension3.value(2), Rank.value(1)).isDefined shouldBe true
		kruskalDimension3(Dimension3.value(2), Rank.value(2)).isDefined shouldBe true
	}
}
