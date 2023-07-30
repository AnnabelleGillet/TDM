package tdm.core.decomposition

import mulot.core.tensordecomposition.cp.Norms

object Norm extends Enumeration {
	type Norm = Norms.Norm
	
	val L1 = Norms.L1
	val L2 = Norms.L2
}