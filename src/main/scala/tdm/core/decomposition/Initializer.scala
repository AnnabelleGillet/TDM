package tdm.core.decomposition

object Initializers extends Enumeration {
	type Intializer = Value
	
	val GAUSSIAN = Value("gaussian")
	val HOSVD = Value("hosvd")
}
