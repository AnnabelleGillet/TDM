package test.tdm.core

import java.sql.{Connection, DriverManager}
import java.util.Properties

import org.apache.spark.sql.SparkSession

import org.scalatest._

import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres
import ru.yandex.qatools.embed.postgresql.distribution.Version

import tdm._
import tdm.core._

class TensorFromDataSourceTest extends FlatSpec with Matchers {
	val user: String = EmbeddedPostgres.DEFAULT_USER
    val password: String = EmbeddedPostgres.DEFAULT_PASSWORD
    val dbName: String = EmbeddedPostgres.DEFAULT_DB_NAME
    val port: Int = 12345
    
    val embeddedPostgres: EmbeddedPostgres = new EmbeddedPostgres(Version.V9_6_11)
    val url: String = embeddedPostgres.start("localhost", port, dbName, user, password)
    
    implicit val sparkSession = SparkSession.builder().master("local[4]").getOrCreate()
	sparkSession.sparkContext.setLogLevel("ERROR")
    
	"A tensor" should "be built from data source" in {
		object User extends TensorDimension[String]
    	object Hashtag extends TensorDimension[String]
    	object Time extends TensorDimension[Long]
		
		val connectionProperties: Properties = new Properties()
		connectionProperties.put("url", url)
		connectionProperties.put("user", user)
		connectionProperties.put("password", password)
		
        val connection: Connection = DriverManager.getConnection(url)
        // Data preparation
        val st = connection.createStatement()
        st.execute("DROP TABLE IF EXISTS tweet")
        st.execute("DROP TABLE IF EXISTS hashtag")
        st.execute("CREATE TABLE tweet(id TEXT PRIMARY KEY, published_hour INTEGER, user_screen_name TEXT)")
        st.execute("CREATE TABLE hashtag(tweet_id TEXT, hashtag TEXT, PRIMARY KEY(tweet_id, hashtag))")
        st.execute("INSERT INTO tweet(id, published_hour, user_screen_name) VALUES('t1', 1, 'u1')")
        st.execute("INSERT INTO tweet(id, published_hour, user_screen_name) VALUES('t2', 1, 'u2')")
        st.execute("INSERT INTO tweet(id, published_hour, user_screen_name) VALUES('t3', 1, 'u1')")
        st.execute("INSERT INTO tweet(id, published_hour, user_screen_name) VALUES('t4', 2, 'u2')")
        st.execute("INSERT INTO tweet(id, published_hour, user_screen_name) VALUES('t5', 2, 'u3')")
        
        st.execute("INSERT INTO hashtag(tweet_id, hashtag) VALUES('t1', 'ht1')")
        st.execute("INSERT INTO hashtag(tweet_id, hashtag) VALUES('t1', 'ht2')")
        st.execute("INSERT INTO hashtag(tweet_id, hashtag) VALUES('t2', 'ht2')")
        st.execute("INSERT INTO hashtag(tweet_id, hashtag) VALUES('t2', 'ht3')")
        st.execute("INSERT INTO hashtag(tweet_id, hashtag) VALUES('t3', 'ht1')")
        st.execute("INSERT INTO hashtag(tweet_id, hashtag) VALUES('t3', 'ht2')")
        st.execute("INSERT INTO hashtag(tweet_id, hashtag) VALUES('t4', 'ht1')")
        st.execute("INSERT INTO hashtag(tweet_id, hashtag) VALUES('t5', 'ht2')")
        st.execute("INSERT INTO hashtag(tweet_id, hashtag) VALUES('t5', 'ht3')")
        st.execute("INSERT INTO hashtag(tweet_id, hashtag) VALUES('t5', 'ht4')")
        
        st.close
        
        val query = """
          SELECT user_screen_name AS user, ht.hashtag AS hashtag, published_hour AS time, COUNT(*) AS value 
          FROM tweet t 
              INNER JOIN hashtag ht ON t.id = ht.tweet_id 
          GROUP BY user_screen_name, hashtag, published_hour
          """
        val tensor = TensorBuilder[Int](connectionProperties)
            .addDimension(User, "user")
            .addDimension(Hashtag, "hashtag")
            .addDimension(Time, "time")
            .build(query, "value")
        connection.close
		
		tensor(User.value("u1"), Hashtag.value("ht1"), Time.value(1)) shouldBe Some(2)
		tensor(User.value("u1"), Hashtag.value("ht2"), Time.value(1)) shouldBe Some(2)
		tensor(User.value("u2"), Hashtag.value("ht1"), Time.value(2)) shouldBe Some(1)
		tensor(User.value("u2"), Hashtag.value("ht2"), Time.value(1)) shouldBe Some(1)
		tensor(User.value("u2"), Hashtag.value("ht3"), Time.value(1)) shouldBe Some(1)
		tensor(User.value("u3"), Hashtag.value("ht2"), Time.value(2)) shouldBe Some(1)
		tensor(User.value("u3"), Hashtag.value("ht3"), Time.value(2)) shouldBe Some(1)
		tensor(User.value("u3"), Hashtag.value("ht4"), Time.value(2)) shouldBe Some(1)
		
		embeddedPostgres.stop()
	}
}
