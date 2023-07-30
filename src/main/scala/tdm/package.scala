
package object tdm {
	import core._
	
	class DimensionMap[K, V]
	
	implicit def dimensionMapAllowed[T, D <: TensorDimension[_]]: DimensionMap[D, Dimension[T]] = new DimensionMap[D, Dimension[T]]
	implicit def dimensionHomogeneMapAllowed[T]: DimensionMap[TensorDimension[T], Dimension[T]] = new DimensionMap[TensorDimension[T], Dimension[T]]
	
	/**
	 * Type class witnessing that T is an authorized type.
	 */
	
	import scala.annotation.implicitNotFound
	import shapeless.=:!=
	
	@implicitNotFound("Type ${T} is not authorized. Types authorized: Double, Float, Long, Int, Short, Byte, Boolean or String.")
	sealed trait AuthorizedType[T] extends Serializable
	
	final object AuthorizedType {
		
		def apply[T](implicit at: AuthorizedType[T]): AuthorizedType[T] = at
		
		implicit def DoubleAuthorized[T](implicit eq: T =:= Double): AuthorizedType[T] = new AuthorizedType[T] {}
		implicit def FloatAuthorized[T](implicit eq: T =:= Float): AuthorizedType[T] = new AuthorizedType[T] {}
		implicit def LongAuthorized[T](implicit eq: T =:= Long): AuthorizedType[T] = new AuthorizedType[T] {}
		implicit def IntAuthorized[T](implicit eq: T =:= Int): AuthorizedType[T] = new AuthorizedType[T] {}
		implicit def ShortAuthorized[T](implicit eq: T =:= Short): AuthorizedType[T] = new AuthorizedType[T] {}
		implicit def ByteAuthorized[T](implicit eq: T =:= Byte): AuthorizedType[T] = new AuthorizedType[T] {}
		implicit def BooleanAuthorized[T](implicit eq: T =:= Boolean): AuthorizedType[T] = new AuthorizedType[T] {}
		implicit def StringAuthorized[T](implicit eq: T =:= String): AuthorizedType[T] = new AuthorizedType[T] {}
	}
	
	/**
	 * Type class witnessing that E is an element of M
	 */
	
	import shapeless.{HList, HNil, ::}
	
	@implicitNotFound("Dimension not found: tdm.ContainsConstraint[${L} ${U}]. This tensor does not contain dimension of type ${U}.")
	trait ContainsConstraint[L <: HList, U] extends Serializable
	
	object ContainsConstraint {
		
		def apply[L <: HList, U](implicit cc: ContainsConstraint[L, U]): ContainsConstraint[L, U] = cc
		
		implicit def hlistContains[H, T <: HList, U](implicit eq: U =:= H): ContainsConstraint[H :: T, U] =
			new ContainsConstraint[H :: T, U] {}
		implicit def hlistNotContains[H, T <: HList, U](implicit nc: T ContainsConstraint U, neq: U =:!= H): ContainsConstraint[H :: T, U] =
			new ContainsConstraint[H :: T, U] {}
	}
	
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
	 distinct: IsDistinctConstraint[L]): ContainsDimensionsConditionConstraint[DL, P] = new ContainsDimensionsConditionConstraint[DL, P] {}
	
	// When only one dimension is given
	implicit def implicitContainsDimensionCondition[DL <: HList, DLC <: HList, P <: Product, T]
	(implicit productToTuple1: ToTuple.Aux[P, T],
	 zipWithDimensionCondition: ZipWithDimensionCondition.Aux[DL, DLC],
	 evL1L: BasisConstraint[T :: HNil, DLC]): ContainsDimensionsConditionConstraint[DL, P] = new ContainsDimensionsConditionConstraint[DL, P] {}
	
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
	 distinct: IsDistinctConstraint[L]): ContainsAllDimensionsConstraint[DL, P] = new ContainsAllDimensionsConstraint[DL, P] {}

	/**
	 * Type class witnessing that all Dimensions and ranks given inside a Product must be contain in the HList,
	 * and that the Product contains all the Dimensions of the HList.
	 */
	@implicitNotFound("All dimensions of the tensor must be given once: tdm.ContainsAllDimensionRankConstraint. Dimensions of this tensor: ${DL}, dimensions given: ${P}.")
	sealed trait ContainsAllDimensionsRankConstraint[DL <: HList, P <: Product]
	
	implicit def implicitContainsAllDimensionsRankConstraintAux[DL <: HList, DLV <: HList, P <: Product, L <: HList]
	(implicit zipWithDimensionRank: ZipWithDimensionRank.Aux[DL, DLV],
	 productToHlistPL1: ToHList.Aux[P, L],
	 evL1L: BasisConstraint[DLV, L],
	 evLL1: BasisConstraint[L, DLV],
	 distinct: IsDistinctConstraint[L]): ContainsAllDimensionsRankConstraint[DL, P] = new ContainsAllDimensionsRankConstraint[DL, P] {}
	
	/**
	 * Type class witnessing that L1 and L2 have exactly the same elements
	 */
	@implicitNotFound("Different schemas: tdm.SameSchemaConstraint[${L1} ${L2}]. The two tensors must have the same schema.")
	trait SameSchemaConstraint[L1 <: HList, L2 <: HList] extends Serializable
	
	object SameSchemaConstraint {
		
		implicit def hlistContains[L1 <: HList, L2 <: HList](implicit evL1L2: BasisConstraint[L1, L2], evL2L1: BasisConstraint[L2, L1]): SameSchemaConstraint[L1, L2] =
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
		
		implicit def hlistContains[H, T <: HList, L2 <: HList](implicit eq: Selector[L2, H]): ContainsAtLeastOneConstraint[H :: T, L2] =
			new ContainsAtLeastOneConstraint[H :: T, L2] {}
		implicit def hlistNotContains[H, T <: HList, L2 <: HList](implicit nc: ContainsAtLeastOneConstraint[T, L2], neq: IsDistinctConstraint[H :: L2]): ContainsAtLeastOneConstraint[H :: T, L2] =
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
		(implicit zipWithDimensionType: ZipWithDimensionType.Aux[T, ZwdtOut]): Aux[H :: T, (H, CT) :: ZwdtOut] = {
			new ZipWithDimensionType[H :: T] { type Out = (H, CT) :: ZwdtOut }
		}*/
		
		implicit def hconsZipWithDimensionTypeDouble[CT, H <: TensorDimension[Double], T <: HList, ZwdtOut <: HList]
		(implicit zipWithDimensionType: ZipWithDimensionType.Aux[T, ZwdtOut], eq: CT =:= Double): Aux[H :: T, (H, CT) :: ZwdtOut] = {
			new ZipWithDimensionType[H :: T] { type Out = (H, CT) :: ZwdtOut }
		}
		
		implicit def hconsZipWithDimensionTypeFloat[CT, H <: TensorDimension[Float], T <: HList, ZwdtOut <: HList]
		(implicit zipWithDimensionType: ZipWithDimensionType.Aux[T, ZwdtOut], eq: CT =:= Float): Aux[H :: T, (H, CT) :: ZwdtOut] = {
			new ZipWithDimensionType[H :: T] { type Out = (H, CT) :: ZwdtOut }
		}
		
		implicit def hconsZipWithDimensionTypeLong[CT, H <: TensorDimension[Long], T <: HList, ZwdtOut <: HList]
		(implicit zipWithDimensionType: ZipWithDimensionType.Aux[T, ZwdtOut], eq: CT =:= Long): Aux[H :: T, (H, CT) :: ZwdtOut] = {
			new ZipWithDimensionType[H :: T] { type Out = (H, CT) :: ZwdtOut }
		}
		
		implicit def hconsZipWithDimensionTypeInt[CT, H <: TensorDimension[Int], T <: HList, ZwdtOut <: HList]
		(implicit zipWithDimensionType: ZipWithDimensionType.Aux[T, ZwdtOut], eq: CT =:= Int): Aux[H :: T, (H, CT) :: ZwdtOut] = {
			new ZipWithDimensionType[H :: T] { type Out = (H, CT) :: ZwdtOut }
		}
		
		implicit def hconsZipWithDimensionTypeShort[CT, H <: TensorDimension[Short], T <: HList, ZwdtOut <: HList]
		(implicit zipWithDimensionType: ZipWithDimensionType.Aux[T, ZwdtOut], eq: CT =:= Short): Aux[H :: T, (H, CT) :: ZwdtOut] = {
			new ZipWithDimensionType[H :: T] { type Out = (H, CT) :: ZwdtOut }
		}
		
		implicit def hconsZipWithDimensionTypeByte[CT, H <: TensorDimension[Byte], T <: HList, ZwdtOut <: HList]
		(implicit zipWithDimensionType: ZipWithDimensionType.Aux[T, ZwdtOut], eq: CT =:= Byte): Aux[H :: T, (H, CT) :: ZwdtOut] = {
			new ZipWithDimensionType[H :: T] { type Out = (H, CT) :: ZwdtOut }
		}
		
		implicit def hconsZipWithDimensionTypeBoolean[CT, H <: TensorDimension[Boolean], T <: HList, ZwdtOut <: HList]
		(implicit zipWithDimensionType: ZipWithDimensionType.Aux[T, ZwdtOut], eq: CT =:= Boolean): Aux[H :: T, (H, CT) :: ZwdtOut] = {
			new ZipWithDimensionType[H :: T] { type Out = (H, CT) :: ZwdtOut }
		}
		
		implicit def hconsZipWithDimensionTypeString[CT, H <: TensorDimension[String], T <: HList, ZwdtOut <: HList]
		(implicit zipWithDimensionType: ZipWithDimensionType.Aux[T, ZwdtOut], eq: CT =:= String): Aux[H :: T, (H, CT) :: ZwdtOut] = {
			new ZipWithDimensionType[H :: T] { type Out = (H, CT) :: ZwdtOut }
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
		
		/*implicit def hconsZipWithDimensionConditionType[CT, H <: TensorDimension[CT], T <: HList, ZwdtOut <: HList]
		(implicit zipWithDimensionType: ZipWithDimensionCondition.Aux[T, ZwdtOut]): Aux[H :: T, (H, CT => Boolean) :: ZwdtOut] = {
			new ZipWithDimensionCondition[H :: T] { type Out = (H, CT => Boolean) :: ZwdtOut }
		}*/
		
		implicit def hconsZipWithDimensionConditionDouble[CT, H <: TensorDimension[Double], T <: HList, ZwdtOut <: HList]
		(implicit zipWithDimensionCondition: ZipWithDimensionCondition.Aux[T, ZwdtOut], eq: CT =:= Double): Aux[H :: T, (H, CT => Boolean) :: ZwdtOut] = {
			new ZipWithDimensionCondition[H :: T] { type Out = (H, CT => Boolean) :: ZwdtOut }
		}
		
		implicit def hconsZipWithDimensionConditionFloat[CT, H <: TensorDimension[Float], T <: HList, ZwdtOut <: HList]
		(implicit zipWithDimensionCondition: ZipWithDimensionCondition.Aux[T, ZwdtOut], eq: CT =:= Float): Aux[H :: T, (H, CT => Boolean) :: ZwdtOut] = {
			new ZipWithDimensionCondition[H :: T] { type Out = (H, CT => Boolean) :: ZwdtOut }
		}
		
		implicit def hconsZipWithDimensionConditionLong[CT, H <: TensorDimension[Long], T <: HList, ZwdtOut <: HList]
		(implicit zipWithDimensionCondition: ZipWithDimensionCondition.Aux[T, ZwdtOut], eq: CT =:= Long): Aux[H :: T, (H, CT => Boolean) :: ZwdtOut] = {
			new ZipWithDimensionCondition[H :: T] { type Out = (H, CT => Boolean) :: ZwdtOut }
		}
		
		implicit def hconsZipWithDimensionConditionInt[CT, H <: TensorDimension[Int], T <: HList, ZwdtOut <: HList]
		(implicit zipWithDimensionCondition: ZipWithDimensionCondition.Aux[T, ZwdtOut], eq: CT =:= Int): Aux[H :: T, (H, CT => Boolean) :: ZwdtOut] = {
			new ZipWithDimensionCondition[H :: T] { type Out = (H, CT => Boolean) :: ZwdtOut }
		}
		
		implicit def hconsZipWithDimensionConditionShort[CT, H <: TensorDimension[Short], T <: HList, ZwdtOut <: HList]
		(implicit zipWithDimensionCondition: ZipWithDimensionCondition.Aux[T, ZwdtOut], eq: CT =:= Short): Aux[H :: T, (H, CT => Boolean) :: ZwdtOut] = {
			new ZipWithDimensionCondition[H :: T] { type Out = (H, CT => Boolean) :: ZwdtOut }
		}
		
		implicit def hconsZipWithDimensionConditionByte[CT, H <: TensorDimension[Byte], T <: HList, ZwdtOut <: HList]
		(implicit zipWithDimensionCondition: ZipWithDimensionCondition.Aux[T, ZwdtOut], eq: CT =:= Byte): Aux[H :: T, (H, CT => Boolean) :: ZwdtOut] = {
			new ZipWithDimensionCondition[H :: T] { type Out = (H, CT => Boolean) :: ZwdtOut }
		}
		
		implicit def hconsZipWithDimensionConditionBoolean[CT, H <: TensorDimension[Boolean], T <: HList, ZwdtOut <: HList]
		(implicit zipWithDimensionCondition: ZipWithDimensionCondition.Aux[T, ZwdtOut], eq: CT =:= Boolean): Aux[H :: T, (H, CT => Boolean) :: ZwdtOut] = {
			new ZipWithDimensionCondition[H :: T] { type Out = (H, CT => Boolean) :: ZwdtOut }
		}
		
		implicit def hconsZipWithDimensionConditionString[CT, H <: TensorDimension[String], T <: HList, ZwdtOut <: HList]
		(implicit zipWithDimensionCondition: ZipWithDimensionCondition.Aux[T, ZwdtOut], eq: CT =:= String): Aux[H :: T, (H, CT => Boolean) :: ZwdtOut] = {
			new ZipWithDimensionCondition[H :: T] { type Out = (H, CT => Boolean) :: ZwdtOut }
		}
	}
	
	/**
	 * Type class mapping a HList L1 of TensorDimension to a HList L2 of (TensorDimension[CT], Int)
	 */
	
	trait ZipWithDimensionRank[L <: HList] extends Serializable {
		type Out <: HList
	}
	
	object ZipWithDimensionRank {
		type Aux[L <: HList, Out0 <: HList] = ZipWithDimensionRank[L] {type Out = Out0}
		
		implicit val hnilZipWithDimensionRank: Aux[HNil, HNil] = new ZipWithDimensionRank[HNil] {
			type Out = HNil
		}
		
		implicit def hconsZipWithDimensionRank[CT, H <: TensorDimension[_], T <: HList, ZwdtOut <: HList]
		(implicit zipWithDimensionRank: ZipWithDimensionRank.Aux[T, ZwdtOut]): Aux[H :: T, (H, Int) :: ZwdtOut] = {
			new ZipWithDimensionRank[H :: T] {
				type Out = (H, Int) :: ZwdtOut
			}
		}
	}
}