package tdm.core

import org.apache.spark.sql.SparkSession
import org.scalatest._

class TensorFromDataFrameTest extends FlatSpec with Matchers {
	"A tensor" should "be built from DataFrame" in {
		object User extends TensorDimension[String]
		object Hashtag extends TensorDimension[String]
		object Time extends TensorDimension[Long]
		
		implicit val sparkSession = SparkSession.builder().master("local[4]").getOrCreate()
		sparkSession.sparkContext.setLogLevel("ERROR")
		import sparkSession.implicits._
		
		val df = Seq[(String, String, Long, Int)](
				("u1", "ht1", 1, 2),
				("u1", "ht2", 1, 2),
				("u2", "ht1", 2, 1),
				("u2", "ht2", 1, 1),
				("u2", "ht3", 1, 1),
				("u3", "ht2", 2, 1),
				("u3", "ht3", 2, 1),
				("u3", "ht4", 2, 1)
			).toDF("user", "hashtag", "time", "value")
		
		val tensor = TensorBuilder[Int](df)
			.addDimension(User, "user")
			.addDimension(Hashtag, "hashtag")
			.addDimension(Time, "time")
			.build("value")
		
		tensor(User.value("u1"), Hashtag.value("ht1"), Time.value(1)) shouldBe Some(2)
		tensor(User.value("u1"), Hashtag.value("ht2"), Time.value(1)) shouldBe Some(2)
		tensor(User.value("u2"), Hashtag.value("ht1"), Time.value(2)) shouldBe Some(1)
		tensor(User.value("u2"), Hashtag.value("ht2"), Time.value(1)) shouldBe Some(1)
		tensor(User.value("u2"), Hashtag.value("ht3"), Time.value(1)) shouldBe Some(1)
		tensor(User.value("u3"), Hashtag.value("ht2"), Time.value(2)) shouldBe Some(1)
		tensor(User.value("u3"), Hashtag.value("ht3"), Time.value(2)) shouldBe Some(1)
		tensor(User.value("u3"), Hashtag.value("ht4"), Time.value(2)) shouldBe Some(1)
	}
}
