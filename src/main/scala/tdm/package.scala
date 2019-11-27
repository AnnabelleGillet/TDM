
package object tdm {
	import core._

    implicit def valueToTuple1[T](v: T): Tuple1[T] = Tuple1(v)
    
    class DimensionMap[K, V]

	implicit def dimensionMapAllowed[T, D <: TensorDimension[_]] = new DimensionMap[D, Dimension[T]]
    implicit def dimensionHomogeneMapAllowed[T] = new DimensionMap[TensorDimension[T], Dimension[T]]
	
	/**
     * Type class witnessing that T is an authorized type.
     */
    
    import scala.annotation.implicitNotFound
    import shapeless.=:!=
    
    @implicitNotFound("Type ${T} is not authorized. Types authorized: Double, Float, Long, Int, Short, Byte, Boolean, Char or String.")
    sealed trait AuthorizedType[T] extends Serializable
    
    final object AuthorizedType {
    
        def apply[T](implicit at: AuthorizedType[T]): AuthorizedType[T] = at
        
        implicit def DoubleAuthorized[T](implicit eq: T =:= Double) = new AuthorizedType[T] {}
        implicit def FloatAuthorized[T](implicit eq: T =:= Float) = new AuthorizedType[T] {}
        implicit def LongAuthorized[T](implicit eq: T =:= Long) = new AuthorizedType[T] {}
        implicit def IntAuthorized[T](implicit eq: T =:= Int) = new AuthorizedType[T] {}
        implicit def ShortAuthorized[T](implicit eq: T =:= Short) = new AuthorizedType[T] {}
        implicit def ByteAuthorized[T](implicit eq: T =:= Byte) = new AuthorizedType[T] {}
        implicit def BooleanAuthorized[T](implicit eq: T =:= Boolean) = new AuthorizedType[T] {}
        implicit def CharAuthorized[T](implicit eq: T =:= Char) = new AuthorizedType[T] {}
        implicit def StringAuthorized[T](implicit eq: T =:= String) = new AuthorizedType[T] {}
    }
    
    /**
     * Type class witnessing that E is an element of M
     */
    
    import shapeless.{HList, HNil, ::}

    @implicitNotFound("Dimension not found: tdm.ContainsConstraint[${L} ${U}]. This tensor does not contain dimension of type ${U}.")
    trait ContainsConstraint[L <: HList, U] extends Serializable

    object ContainsConstraint {

        def apply[L <: HList, U](implicit cc: ContainsConstraint[L, U]): ContainsConstraint[L, U] = cc
        
        implicit def hlistContains[H, T <: HList, U](implicit eq: U =:= H) =
            new ContainsConstraint[H :: T, U] {}
        implicit def hlistNotContains[H, T <: HList, U](implicit nc: T ContainsConstraint U, neq: U =:!= H) =
            new ContainsConstraint[H :: T, U] {}
    }
    
    /**
     * Type class witnessing that all Dimensions' condition given inside a Product must be contain in the HList.
     */
    import shapeless.{BasisConstraint, IsDistinctConstraint}
    import shapeless.ops.product.ToHList
    
    @implicitNotFound("All the dimensions' condition given must be part of the tensor: tdm.ContainsDimensionsConditionConstraint. Dimensions' condition of this tensor: ${L}, dimensions' condition given: ${P}.")
    sealed trait ContainsDimensionsConditionConstraint[P <: Product, L <: HList]
    
    implicit def implicitAux[P <: Product, L <: HList, L1 <: HList]
        (implicit productToHlist: ToHList.Aux[P, L1], 
        		evL1L: BasisConstraint[L1, L], 
        		distinct: IsDistinctConstraint[L1]) = new ContainsDimensionsConditionConstraint[P, L] {}
    
    /**
     * Type class witnessing that all Dimensions given inside a Product must be contain in the HList, 
     * and that the Product contains all the Dimensions of the HList.
     */    
    @implicitNotFound("All dimensions of the tensor must be given once: tdm.ContainsAllDimensionConstraint. Dimensions of this tensor: ${PE}, dimensions given: ${P}.")
    sealed trait ContainsAllDimensionsConstraint[PE <: Product, P <: Product, L <: HList]
    
    implicit def implicitContainsAllDimensionsConstraintAux[PE <: Product, P <: Product, L <: HList, L1 <: HList]
        (implicit productToHlist: ToHList.Aux[P, L1],
        		evL1L: BasisConstraint[L1, L], 
        		evLL1: BasisConstraint[L, L1],
        		distinct: IsDistinctConstraint[L1]) = new ContainsAllDimensionsConstraint[PE, P, L] {}
    
    /**
     * Type class witnessing that at least one element of L1 is an element of L2
     */
    import shapeless.ops.hlist.Selector
    
    @implicitNotFound("Dimension not found: tdm.ContainsAtLeastOneConstraint[${L1} ${L2}]. The two tensors must have at least one common dimension.")
    trait ContainsAtLeastOneConstraint[L1 <: HList, L2 <: HList] extends Serializable

    object ContainsAtLeastOneConstraint {

        def apply[L1 <: HList, L2 <: HList](implicit cc: ContainsAtLeastOneConstraint[L1, L2]): ContainsAtLeastOneConstraint[L1, L2] = cc
        
        implicit def hlistContains[H, T <: HList, L2 <: HList](implicit eq: Selector[L2, H]) =
            new ContainsAtLeastOneConstraint[H :: T, L2] {}
        implicit def hlistNotContains[H, T <: HList, L2 <: HList](implicit nc: ContainsAtLeastOneConstraint[T, L2], neq: IsDistinctConstraint[H :: L2]) =
            new ContainsAtLeastOneConstraint[H :: T, L2] {}
    }
    
    /**
     * Type class producing an union on 2 Product
     */
    import shapeless.ops.hlist.{Tupler, Union}
    
    sealed trait UnionProduct[P1 <: Product, P2 <: Product, POut <: Product]
    
    implicit def implicitUnionProductAux[P1 <: Product, P2 <: Product, POut <: Product, L1 <: HList, L2 <: HList, LOut <: HList]
        (implicit productToHlist1: ToHList.Aux[P1, L1],
        		productToHlist2: ToHList.Aux[P2, L2],
        		unionHlists: Union.Aux[L1, L2, LOut],
        		hlistOutToProductOut: Tupler.Aux[LOut, POut]) = new UnionProduct[P1, P2, POut] {}
}