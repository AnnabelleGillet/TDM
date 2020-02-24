
package object tdm {
	import core._
    
    class DimensionMap[K, V]

	implicit def dimensionMapAllowed[T, D <: TensorDimension[_]] = new DimensionMap[D, Dimension[T]]
    implicit def dimensionHomogeneMapAllowed[T] = new DimensionMap[TensorDimension[T], Dimension[T]]
	
	/**
     * Type class witnessing that T is an authorized type.
     */
    
    import scala.annotation.implicitNotFound
    import shapeless.=:!=
    
    @implicitNotFound("Type ${T} is not authorized. Types authorized: Double, Float, Long, Int, Short, Byte, Boolean or String.")
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
     * Type class adding an element to an HList and removing another element
     */
    /*import shapeless.IsDistinctConstraint
    import shapeless.ops.hlist.Partition
    
    //@implicitNotFound("Dimension not found: tdm.ContainsConstraint[${L} ${U}]. This tensor does not contain dimension of type ${U}.")
    trait RenameDimensionConstraint[L <: HList, ER, EA] extends Serializable {
    	type Out <: HList
    }
    
    object RenameDimensionConstraint {
    	type Aux[L <: HList, ER, EA, Out0 <: HList] = RenameDimensionConstraint[L, ER, EA] { type Out = Out0 }

        def apply[L <: HList, ER, EA, LOut <: HList](implicit newDimensionConstraint: IsDistinctConstraint[EA :: L],
            	newTypeList: Partition.Aux[EA :: L, ER, ER :: HNil, LOut]): RenameDimensionConstraint[L, ER, EA] = 
        	new RenameDimensionConstraint[L, ER, EA] { type Out = LOut }
    }*/
    
    /**
     * Type class witnessing that all Dimensions' condition given inside a Product must be contain in the HList.
     */
    import shapeless.{BasisConstraint, IsDistinctConstraint}
    import shapeless.ops.product.{ToHList, ToTuple}
    
    @implicitNotFound("All the dimensions' condition given must be part of the tensor: tdm.ContainsDimensionsConditionConstraint. Dimensions' of this tensor: ${DL}, dimensions' condition given: ${P}.")
    sealed trait ContainsDimensionsConditionConstraint[DL <: HList, P <: Product]
    
    implicit def implicitContainsDimensionsCondition[DL <: HList, DLC <: HList, P <: Product, L <: HList]
        (implicit productToHlist: ToHList.Aux[P, L], 
        		zipWithDimensionCondition: ZipWithDimensionCondition.Aux[DL, DLC],
        		evL1L: BasisConstraint[L, DLC], 
        		distinct: IsDistinctConstraint[L]) = new ContainsDimensionsConditionConstraint[DL, P] {}
    
    // When only one dimension is given
    implicit def implicitContainsDimensionCondition[DL <: HList, DLC <: HList, P <: Product, T]
        (implicit productToTuple1: ToTuple.Aux[P, T],
        		zipWithDimensionCondition: ZipWithDimensionCondition.Aux[DL, DLC],
        		evL1L: BasisConstraint[T :: HNil, DLC]) = new ContainsDimensionsConditionConstraint[DL, P] {}
    
    /**
     * Type class witnessing that all Dimensions given inside a Product must be contain in the HList, 
     * and that the Product contains all the Dimensions of the HList.
     */    
    @implicitNotFound("All dimensions of the tensor must be given once: tdm.ContainsAllDimensionConstraint. Dimensions of this tensor: ${DL}, dimensions given: ${P}.")
    sealed trait ContainsAllDimensionsConstraint[DL <: HList, P <: Product]
    
    implicit def implicitContainsAllDimensionsConstraintAux[DL <: HList, DLV <: HList, P <: Product, L <: HList]
        (implicit zipWithDimensionType: ZipWithDimensionType.Aux[DL, DLV],
        		productToHlistPL1: ToHList.Aux[P, L],
        		evL1L: BasisConstraint[DLV, L], 
        		evLL1: BasisConstraint[L, DLV],
        		distinct: IsDistinctConstraint[L]) = new ContainsAllDimensionsConstraint[DL, P] {}
    
    /**
     * Type class witnessing that L1 and L2 have exactly the same elements
     */
    @implicitNotFound("Different schemas: tdm.SameSchemaConstraint[${L1} ${L2}]. The two tensors must have the same schema.")
    trait SameSchemaConstraint[L1 <: HList, L2 <: HList] extends Serializable

    object SameSchemaConstraint {

        //def apply[L1 <: HList, L2 <: HList](implicit cc: SameSchemaConstraint[L1, L2]): SameSchemaConstraint[L1, L2] = cc
        
        implicit def hlistContains[L1 <: HList, L2 <: HList](implicit evL1L2: BasisConstraint[L1, L2], evL2L1: BasisConstraint[L2, L1]) =
            new SameSchemaConstraint[L1, L2] {}
    }
    
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
     * Type class mapping a HList L1 of TensorDimension to a HList L2 of (TensorDimension[CT], CT)
     */
    
    trait ZipWithDimensionType[L <: HList] extends Serializable {
    	type Out <: HList
    }
    
    object ZipWithDimensionType {
    	type Aux[L <: HList, Out0 <: HList] = ZipWithDimensionType[L] { type Out = Out0 }
    	
    	implicit val hnilZipWithDimensionType: Aux[HNil, HNil] = new ZipWithDimensionType[HNil] {
    		type Out = HNil
    	}
    	
    	/*implicit def hconsZipWithDimensionType[CT, H <: TensorDimension[CT], T <: HList, ZwdtOut <: HList]
    	(implicit zipWithDimensionType: ZipWithDimensionType.Aux[T, ZwdtOut]): Aux[H :: T, Tuple2[H, CT] :: ZwdtOut] = {
    		new ZipWithDimensionType[H :: T] { type Out = Tuple2[H, CT] :: ZwdtOut }
    	}*/
    	
        implicit def hconsZipWithDimensionTypeDouble[CT, H <: TensorDimension[Double], T <: HList, ZwdtOut <: HList]
    	(implicit zipWithDimensionType: ZipWithDimensionType.Aux[T, ZwdtOut], eq: CT =:= Double): Aux[H :: T, Tuple2[H, CT] :: ZwdtOut] = {
    		new ZipWithDimensionType[H :: T] { type Out = Tuple2[H, CT] :: ZwdtOut }
    	}
        
        implicit def hconsZipWithDimensionTypeFloat[CT, H <: TensorDimension[Float], T <: HList, ZwdtOut <: HList]
    	(implicit zipWithDimensionType: ZipWithDimensionType.Aux[T, ZwdtOut], eq: CT =:= Float): Aux[H :: T, Tuple2[H, CT] :: ZwdtOut] = {
    		new ZipWithDimensionType[H :: T] { type Out = Tuple2[H, CT] :: ZwdtOut }
    	}
        
    	implicit def hconsZipWithDimensionTypeLong[CT, H <: TensorDimension[Long], T <: HList, ZwdtOut <: HList]
    	(implicit zipWithDimensionType: ZipWithDimensionType.Aux[T, ZwdtOut], eq: CT =:= Long): Aux[H :: T, Tuple2[H, CT] :: ZwdtOut] = {
    		new ZipWithDimensionType[H :: T] { type Out = Tuple2[H, CT] :: ZwdtOut }
    	}
    	
    	implicit def hconsZipWithDimensionTypeInt[CT, H <: TensorDimension[Int], T <: HList, ZwdtOut <: HList]
    	(implicit zipWithDimensionType: ZipWithDimensionType.Aux[T, ZwdtOut], eq: CT =:= Int): Aux[H :: T, Tuple2[H, CT] :: ZwdtOut] = {
    		new ZipWithDimensionType[H :: T] { type Out = Tuple2[H, CT] :: ZwdtOut }
    	}
    	
    	implicit def hconsZipWithDimensionTypeShort[CT, H <: TensorDimension[Short], T <: HList, ZwdtOut <: HList]
    	(implicit zipWithDimensionType: ZipWithDimensionType.Aux[T, ZwdtOut], eq: CT =:= Short): Aux[H :: T, Tuple2[H, CT] :: ZwdtOut] = {
    		new ZipWithDimensionType[H :: T] { type Out = Tuple2[H, CT] :: ZwdtOut }
    	}
    	
    	implicit def hconsZipWithDimensionTypeByte[CT, H <: TensorDimension[Byte], T <: HList, ZwdtOut <: HList]
    	(implicit zipWithDimensionType: ZipWithDimensionType.Aux[T, ZwdtOut], eq: CT =:= Byte): Aux[H :: T, Tuple2[H, CT] :: ZwdtOut] = {
    		new ZipWithDimensionType[H :: T] { type Out = Tuple2[H, CT] :: ZwdtOut }
    	}
    	
    	implicit def hconsZipWithDimensionTypeBoolean[CT, H <: TensorDimension[Boolean], T <: HList, ZwdtOut <: HList]
    	(implicit zipWithDimensionType: ZipWithDimensionType.Aux[T, ZwdtOut], eq: CT =:= Boolean): Aux[H :: T, Tuple2[H, CT] :: ZwdtOut] = {
    		new ZipWithDimensionType[H :: T] { type Out = Tuple2[H, CT] :: ZwdtOut }
    	}
    	
    	implicit def hconsZipWithDimensionTypeString[CT, H <: TensorDimension[String], T <: HList, ZwdtOut <: HList]
    	(implicit zipWithDimensionType: ZipWithDimensionType.Aux[T, ZwdtOut], eq: CT =:= String): Aux[H :: T, Tuple2[H, CT] :: ZwdtOut] = {
    		new ZipWithDimensionType[H :: T] { type Out = Tuple2[H, CT] :: ZwdtOut }
    	}
    }
    
    /**
     * Type class mapping a HList L1 of TensorDimension to a HList L2 of (TensorDimension[CT], CT => Boolean)
     */
    
    trait ZipWithDimensionCondition[L <: HList] extends Serializable {
    	type Out <: HList
    }
    
    object ZipWithDimensionCondition {
    	type Aux[L <: HList, Out0 <: HList] = ZipWithDimensionCondition[L] { type Out = Out0 }
    	
    	implicit val hnilZipWithDimensionCondition: Aux[HNil, HNil] = new ZipWithDimensionCondition[HNil] {
    		type Out = HNil
    	}
    	
    	/*implicit def hconsZipWithDimensionType[CT, H <: TensorDimension[CT], T <: HList, ZwdtOut <: HList]
    	(implicit zipWithDimensionType: ZipWithDimensionType.Aux[T, ZwdtOut]): Aux[H :: T, Tuple2[H, CT] :: ZwdtOut] = {
    		new ZipWithDimensionType[H :: T] { type Out = Tuple2[H, CT] :: ZwdtOut }
    	}*/
    	
        implicit def hconsZipWithDimensionConditionDouble[CT, H <: TensorDimension[Double], T <: HList, ZwdtOut <: HList]
    	(implicit zipWithDimensionCondition: ZipWithDimensionCondition.Aux[T, ZwdtOut], eq: CT =:= Double): Aux[H :: T, Tuple2[H, CT => Boolean] :: ZwdtOut] = {
    		new ZipWithDimensionCondition[H :: T] { type Out = Tuple2[H, CT => Boolean] :: ZwdtOut }
    	}
        
        implicit def hconsZipWithDimensionConditionFloat[CT, H <: TensorDimension[Float], T <: HList, ZwdtOut <: HList]
    	(implicit zipWithDimensionCondition: ZipWithDimensionCondition.Aux[T, ZwdtOut], eq: CT =:= Float): Aux[H :: T, Tuple2[H, CT => Boolean] :: ZwdtOut] = {
    		new ZipWithDimensionCondition[H :: T] { type Out = Tuple2[H, CT => Boolean] :: ZwdtOut }
    	}
        
    	implicit def hconsZipWithDimensionConditionLong[CT, H <: TensorDimension[Long], T <: HList, ZwdtOut <: HList]
    	(implicit zipWithDimensionCondition: ZipWithDimensionCondition.Aux[T, ZwdtOut], eq: CT =:= Long): Aux[H :: T, Tuple2[H, CT => Boolean] :: ZwdtOut] = {
    		new ZipWithDimensionCondition[H :: T] { type Out = Tuple2[H, CT => Boolean] :: ZwdtOut }
    	}
    	
    	implicit def hconsZipWithDimensionConditionInt[CT, H <: TensorDimension[Int], T <: HList, ZwdtOut <: HList]
    	(implicit zipWithDimensionCondition: ZipWithDimensionCondition.Aux[T, ZwdtOut], eq: CT =:= Int): Aux[H :: T, Tuple2[H, CT => Boolean] :: ZwdtOut] = {
    		new ZipWithDimensionCondition[H :: T] { type Out = Tuple2[H, CT => Boolean] :: ZwdtOut }
    	}
    	
    	implicit def hconsZipWithDimensionConditionShort[CT, H <: TensorDimension[Short], T <: HList, ZwdtOut <: HList]
    	(implicit zipWithDimensionCondition: ZipWithDimensionCondition.Aux[T, ZwdtOut], eq: CT =:= Short): Aux[H :: T, Tuple2[H, CT => Boolean] :: ZwdtOut] = {
    		new ZipWithDimensionCondition[H :: T] { type Out = Tuple2[H, CT => Boolean] :: ZwdtOut }
    	}
    	
    	implicit def hconsZipWithDimensionConditionByte[CT, H <: TensorDimension[Byte], T <: HList, ZwdtOut <: HList]
    	(implicit zipWithDimensionCondition: ZipWithDimensionCondition.Aux[T, ZwdtOut], eq: CT =:= Byte): Aux[H :: T, Tuple2[H, CT => Boolean] :: ZwdtOut] = {
    		new ZipWithDimensionCondition[H :: T] { type Out = Tuple2[H, CT => Boolean] :: ZwdtOut }
    	}
    	
    	implicit def hconsZipWithDimensionConditionBoolean[CT, H <: TensorDimension[Boolean], T <: HList, ZwdtOut <: HList]
    	(implicit zipWithDimensionCondition: ZipWithDimensionCondition.Aux[T, ZwdtOut], eq: CT =:= Boolean): Aux[H :: T, Tuple2[H, CT => Boolean] :: ZwdtOut] = {
    		new ZipWithDimensionCondition[H :: T] { type Out = Tuple2[H, CT => Boolean] :: ZwdtOut }
    	}
    	
    	implicit def hconsZipWithDimensionConditionString[CT, H <: TensorDimension[String], T <: HList, ZwdtOut <: HList]
    	(implicit zipWithDimensionCondition: ZipWithDimensionCondition.Aux[T, ZwdtOut], eq: CT =:= String): Aux[H :: T, Tuple2[H, CT => Boolean] :: ZwdtOut] = {
    		new ZipWithDimensionCondition[H :: T] { type Out = Tuple2[H, CT => Boolean] :: ZwdtOut }
    	}
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