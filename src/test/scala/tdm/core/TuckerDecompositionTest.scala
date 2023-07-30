package tdm.core

import org.apache.spark.sql.SparkSession
import org.scalatest.{FlatSpec, Matchers}

class TuckerDecompositionTest extends FlatSpec with Matchers {
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
	
	"A Tensor" should "run the Tucker decomposition" in {
		tensor.tuckerDecomposition(Dimension1.rank(2), Dimension2.rank(2), Dimension3.rank(2)).withMaxIterations(1).execute()
	}
	
	val rankDimension1 = 2
	val rankDimension2 = 2
	val rankDimension3 = 2
	val tuckerResult = tensor.tuckerDecomposition(Dimension1.rank(rankDimension1), Dimension2.rank(rankDimension2), Dimension3.rank(rankDimension3)).withMaxIterations(1).execute()
	
	"The result tensor" should "be valid" in {
		tuckerResult.coreTensor.data.count() shouldBe rankDimension1 * rankDimension2 * rankDimension3
		tuckerResult.factorMatrices.size shouldBe 3
	}
	
	it should "contain values for the original dimension 1" in {
		val kruskalDimension1 = tuckerResult.extractFactorMatrix(Dimension1)
		
		kruskalDimension1.count() shouldBe rankDimension1 * 2
		kruskalDimension1(Dimension1.value("d11"), tuckerResult.Rank.value(0)).isDefined shouldBe true
		kruskalDimension1(Dimension1.value("d11"), tuckerResult.Rank.value(1)).isDefined shouldBe true
		kruskalDimension1(Dimension1.value("d11"), tuckerResult.Rank.value(2)).isDefined shouldBe false
		kruskalDimension1(Dimension1.value("d12"), tuckerResult.Rank.value(0)).isDefined shouldBe true
		kruskalDimension1(Dimension1.value("d12"), tuckerResult.Rank.value(1)).isDefined shouldBe true
		kruskalDimension1(Dimension1.value("d12"), tuckerResult.Rank.value(2)).isDefined shouldBe false
	}
	
	it should "contain values for the original dimension 2" in {
		val kruskalDimension2 = tuckerResult.extractFactorMatrix(Dimension2)
		
		kruskalDimension2.count() shouldBe rankDimension2 * 2
		kruskalDimension2(Dimension2.value("d21"), tuckerResult.Rank.value(0)).isDefined shouldBe true
		kruskalDimension2(Dimension2.value("d21"), tuckerResult.Rank.value(1)).isDefined shouldBe true
		kruskalDimension2(Dimension2.value("d21"), tuckerResult.Rank.value(2)).isDefined shouldBe false
		kruskalDimension2(Dimension2.value("d22"), tuckerResult.Rank.value(0)).isDefined shouldBe true
		kruskalDimension2(Dimension2.value("d22"), tuckerResult.Rank.value(1)).isDefined shouldBe true
		kruskalDimension2(Dimension2.value("d22"), tuckerResult.Rank.value(2)).isDefined shouldBe false
	}
	
	it should "contain values for the original dimension 3" in {
		val kruskalDimension3 = tuckerResult.extractFactorMatrix(Dimension3)
		
		kruskalDimension3.count() shouldBe rankDimension3 * 2
		kruskalDimension3(Dimension3.value(1), tuckerResult.Rank.value(0)).isDefined shouldBe true
		kruskalDimension3(Dimension3.value(1), tuckerResult.Rank.value(1)).isDefined shouldBe true
		kruskalDimension3(Dimension3.value(1), tuckerResult.Rank.value(2)).isDefined shouldBe false
		kruskalDimension3(Dimension3.value(2), tuckerResult.Rank.value(0)).isDefined shouldBe true
		kruskalDimension3(Dimension3.value(2), tuckerResult.Rank.value(1)).isDefined shouldBe true
		kruskalDimension3(Dimension3.value(2), tuckerResult.Rank.value(2)).isDefined shouldBe false
	}
	
	it should "contain values for the core tensor" in {
		tuckerResult.extractValueOfCoreTensor(Dimension1.rank(0), Dimension2.rank(0), Dimension3.rank(0)) shouldBe tuckerResult.extractValueOfCoreTensor(Dimension2.rank(0), Dimension1.rank(0), Dimension3.rank(0))
		tuckerResult.extractValueOfCoreTensor(Dimension1.rank(0), Dimension2.rank(0), Dimension3.rank(0)) shouldBe tuckerResult.extractValueOfCoreTensor(Dimension3.rank(0), Dimension2.rank(0), Dimension1.rank(0))
		tuckerResult.extractValueOfCoreTensor(Dimension1.rank(2), Dimension2.rank(0), Dimension3.rank(0)) shouldBe None
		for (rank1 <- 0 until rankDimension1;
			 rank2 <- 0 until rankDimension2;
			 rank3 <- 0 until rankDimension3) {
			tuckerResult.extractValueOfCoreTensor(Dimension1.rank(rank1), Dimension2.rank(rank2), Dimension3.rank(rank3)).isDefined shouldBe true
		}
	}
}
