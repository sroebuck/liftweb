package net.liftweb.mapper

/*
 * Copyright 2006-2009 WorldWide Conferencing, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */

import _root_.scala.collection.mutable._
import _root_.java.lang.reflect.Method
import _root_.java.sql.{ResultSet, Types}
import _root_.scala.xml.{Text, Node, NodeSeq, Group,
                         Elem, Null, PrefixedAttribute, MetaData}
import _root_.java.util.Date
import _root_.net.liftweb.http.{S, SHtml, FieldError, FieldIdentifier}
import _root_.net.liftweb.http.S._
import _root_.net.liftweb.http.js._
import _root_.net.liftweb.util._
import Helpers._

/**
 * The base (not Typed) trait that defines a field that is mapped to a column or more than 1 column
 * (e.g., MappedPassword) in the database
 */
@serializable
trait BaseMappedField extends SelectableField with Bindable {
  /**
   * Get a JDBC friendly representation of the named field (this is used for MappedFields that correspond to more than
   * 1 column in the database.)
   * @param field -- the name of the field being mapped to
   */
  def jdbcFriendly(field : String): AnyRef

  /**
   * Get a JDBC friendly object for the part of this field that maps to the first
   * column in the database
   */
  def jdbcFriendly: AnyRef

  /**
   * Get the JDBC SQL Type for this field
   */
  def targetSQLType(field: String): Int

  /**
   * Get the JDBC SQL Type for this field
   */
  def targetSQLType: Int

  /**
   * Validate this field and return a list of Validation Issues
   */
  def validate: List[FieldError]

  /**
   * Given the driver type, return the string required to create the column in the database
   */
  def fieldCreatorString(dbType: DriverType, colName: String): String

  /**
   * Given the driver type, return a list of statements to create the columns in the database
   */
  def fieldCreatorString(dbType: DriverType): List[String]

  /**
   * The human name of this field
   */
  def name: String

  /**
   * Convert the field to its name/value pair (e.g., name=David)
   */
  def asString: String

  /**
   * The number of database columns that this field represents
   */
  def dbColumnCount: Int


  def dbColumnNames(in: String): List[String]

  def dbColumnName: String

  /**
   * Should the field be indexed?
   */
  def dbIndexed_? : Boolean

  /**
   * Is the field the table's primary key
   */
  def dbPrimaryKey_? : Boolean

  /**
   * Is the field a foreign key reference
   */
  def dbForeignKey_? : Boolean

  /**
   * Called when a column has been added to the database via Schemifier
   */
  def dbAddedColumn: Box[() => Unit]

  /**
   * Called when a column has indexed via Schemifier
   */
  def dbAddedIndex: Box[() => Unit]

  /**
   * Create an input field for the item
   */
  def toForm: Box[NodeSeq]

  /**
   * A unique 'id' for the field for form generation
   */
  def fieldId: Option[NodeSeq] = None

  def displayNameHtml: Box[NodeSeq] = Empty

  def displayHtml: NodeSeq = displayNameHtml openOr Text(displayName)

  /**
   * This is where the instance creates its "toForm" stuff.
   * The actual toForm method wraps the information based on
   * mode.
   */
  def _toForm: Box[NodeSeq]

  def asHtml: NodeSeq

  /**
   * Called after the field is saved to the database
   */
  protected[mapper] def doneWithSave()

  /**
   * The display name of this field (e.g., "First Name")
   */
  def displayName: String

  def asJsExp: JsExp

  def asJs: List[(String, JsExp)] = List((name, asJsExp))

  def renderJs_? = true
}

/**
 * Mix this trait into a BaseMappedField and it will be indexed
 */
trait DBIndexed extends BaseMappedField {
  override def dbIndexed_? = true
}

/**
 * The Trait that defines a field that is mapped to a foreign key
 */
trait MappedForeignKey[KeyType, MyOwner <: Mapper[MyOwner], Other <: KeyedMapper[KeyType, Other]] extends MappedField[KeyType, MyOwner] {
  type FieldType <: KeyType
  // type ForeignType <: KeyedMapper[KeyType, Other]

  override def equals(other: Any) = other match {
    case km: KeyedMapper[KeyType, Other] => this.is == km.primaryKeyField.is
    case _ => super.equals(other)
  }

  def dbKeyToTable: KeyedMetaMapper[KeyType, Other]

  def validSelectValues: Box[List[(KeyType, String)]] = Empty


  def immutableMsg: NodeSeq = Text(?("Can't change"))

  override def _toForm: Box[NodeSeq] = Full(validSelectValues.flatMap{
      case Nil => Empty

      case xs =>
        val mapBack: HashMap[String, KeyType] = new HashMap
        var selected: Box[String] = Empty

        Full(SHtml.selectObj(xs, Full(this.is), this.set))
    }.openOr(immutableMsg))

  /**
   * Is the key defined
   */
  def defined_? : Boolean

  /**
   * Is the obj field cached
   */
  def cached_? : Boolean = synchronized{ _calcedObj}

  /**
   * Load and cache the record that this field references
   */
  def obj: Box[Other] = synchronized {
    if (!_calcedObj) {
      _calcedObj = true
      this._obj = if(defined_?) dbKeyToTable.find(i_is_!) else Empty
    }
    _obj
  }

  /**
   * Prime the reference of this FK reference
   */
  def primeObj(obj: Box[Other]) = synchronized {
    _obj
    _calcedObj = true
  }

  private var _obj: Box[Other] = Empty
  private var _calcedObj = false
}

trait BaseOwnedMappedField[OwnerType <: Mapper[OwnerType]] extends BaseMappedField

trait TypedField[FieldType] {
  /**
   * The default value for the field
   */
  def defaultValue: FieldType


  /**
   * What is the real class that corresponds to FieldType
   */
  def dbFieldClass: Class[FieldType]
}


/**
 * The strongly typed field that's mapped to a column (or many columns) in the database.
 * FieldType is the type of the field and OwnerType is the Owner of the field
 */
trait MappedField[FieldType <: Any,OwnerType <: Mapper[OwnerType]] extends TypedField[FieldType] with BaseOwnedMappedField[OwnerType] with FieldIdentifier {
  /**
   * Should the field be ignored by the OR Mapper?
   */
  def ignoreField_? = false



  /**
   * Get the field that this prototypical field represents
   *
   * @param actual the object to find the field on
   */
  def actualField(actual: OwnerType): MappedField[FieldType, OwnerType] = actual.getSingleton.getActualField(actual, this)

  /**
   * Given the driver type, return the string required to create the column in the database
   */
  def fieldCreatorString(dbType: DriverType, colName: String): String

  /**
   * Given the driver type, return a list of SQL creation strings for the columns represented by this field
   */
  def fieldCreatorString(dbType: DriverType): List[String] = dbColumnNames(name).map{c => fieldCreatorString(dbType, c)}

  /**
   * Is the field dirty
   */
  private var _dirty_? = false

  /**
   * Is the field dirty (has it been changed since the record was loaded from the database
   */
  def dirty_? = !dbPrimaryKey_? && _dirty_?

  /**
   * Make the field dirty
   */
  protected def dirty_?(b: Boolean) = _dirty_? = b

  /**
   * Called when a column has been added to the database via Schemifier
   */
  def dbAddedColumn: Box[() => Unit] = Empty

  /**
   * Called when a column has indexed via Schemifier
   */
  def dbAddedIndex: Box[() => Unit] = Empty

  /**
   * override this method in indexed fields to indicate that the field has been saved
   */
  def dbIndexFieldIndicatesSaved_? = false;

  /**
   * Return the owner of this field
   */
  def fieldOwner: OwnerType

  /**
   * Are we in "safe" mode (i.e., the value of the field can be read or written without any security checks.)
   */
  final def safe_? : Boolean = fieldOwner.safe_?

  /**
   * Given the current execution state, can the field be written?
   */
  def writePermission_? = false

  /**
   * Given the current execution state, can the field be read?
   */
  def readPermission_? = false

  /**
   * Assignment from the underlying type.  It's ugly, but:<br />
   * field() = new_value <br />
   * field := new_value <br />
   * field set new_value <br />
   * field.set(new_value) <br />
   * are all the same
   */
  def update[Q <% FieldType](v: Q) {
    this.set(v)
  }

  def apply[Q <% FieldType](v: Q): OwnerType = {
    this.set(v)
    fieldOwner
  }

  private var _name : String = null

  /**
   * The internal name of this field.  Use name
   */
  private[mapper] final def i_name_! = _name

  /**
   * The name of this field
   */
  final def name = synchronized {
    if (_name eq null) {
      fieldOwner.checkNames
    }
    _name
  }

  /**
   * Set the name of this field
   */
  private[mapper] final def setName_!(newName : String) : String = {
    if(safe_?) _name = newName.toLowerCase
    _name
  }

  /**
   * The display name of this field (e.g., "First Name")
   */
  override def displayName: String = name

  def resetDirty {
    if (safe_?) dirty_?(false)
  }

  def dbDisplay_? = true

  /**
   * pascal-style assignment for syntactic sugar
   */
  /*
   def ::=(v : Any) : T
   */

  /**
   *  Attempt to figure out what the incoming value is and set the field to that value.  Return true if
   * the value could be assigned
   */
  def setFromAny(value: Any): FieldType

  def toFormAppendedAttributes: MetaData =
  if (Props.mode == Props.RunModes.Test)
  new PrefixedAttribute("lift", "field_name", Text(calcFieldName), Null)
  else Null

  def calcFieldName: String = fieldOwner.getSingleton._dbTableName+":"+name


  final def toForm: Box[NodeSeq] = {
    def mf(in: Node): NodeSeq = in match {
      case g: Group => g.nodes.flatMap(mf)
      case e: Elem => e % toFormAppendedAttributes
      case other => other
    }

    _toForm.map(_.flatMap(mf) )
  }

  /**
   * Create an input field for the item
   */
  override def _toForm: Box[NodeSeq] =
  S.fmapFunc({s: List[String] => this.setFromAny(s)}){funcName =>
  Full(<input type='text' id={fieldId}
      name={funcName}
      value={is match {case null => "" case s => s.toString}}/>)
  }

  /**
   * Set the field to the value
   */
  def set(value: FieldType): FieldType = {
    if (safe_? || writePermission_?) i_set_!(value)
    else throw new Exception("Do not have permissions to set this field")
  }

  /**
   * Set the field to the Box value if the Box is Full
   */
  def set_?(value: Box[FieldType]): Box[FieldType] = {
    value.foreach(v => this.set(v))
    value
  }

  /**
   * A list of functions that transform the value before it is set.  The transformations
   * are also applied before the value is used in a query.  Typical applications
   * of this are trimming and/or toLowerCase-ing strings
   */
  protected def setFilter: List[FieldType => FieldType] = Nil

  protected final def i_set_!(value: FieldType): FieldType = {
    real_i_set_!(runFilters(value, setFilter))
  }

  def runFilters(in: FieldType, filter: List[FieldType => FieldType]): FieldType =
  filter match {
    case Nil => in
    case x :: xs => runFilters(x(in), xs)
  }

  /**
   * Must be implemented to store the value of the field
   */
  protected def real_i_set_!(value: FieldType): FieldType

  def buildSetActualValue(accessor: Method, inst : AnyRef, columnName : String) : (OwnerType, AnyRef) => Unit
  def buildSetLongValue(accessor: Method, columnName: String): (OwnerType, Long, Boolean) => Unit
  def buildSetStringValue(accessor: Method, columnName: String): (OwnerType, String) => Unit
  def buildSetDateValue(accessor: Method, columnName: String): (OwnerType, Date) => Unit
  def buildSetBooleanValue(accessor: Method, columnName: String) : (OwnerType, Boolean, Boolean) => Unit
  protected def getField(inst: OwnerType, meth: Method) = meth.invoke(inst).asInstanceOf[MappedField[FieldType,OwnerType]];
  protected def doField(inst: OwnerType, meth: Method, func: PartialFunction[MappedField[FieldType, OwnerType], Unit]) {
    val f = getField(inst, meth)
    if (func.isDefinedAt(f)) func(f)
  }
  /**
   * Convert the field to its "context free" type (e.g., String, Int, Long, etc.)
   * If there are no read permissions, the value will be obscured
   */
  def is: FieldType = {
    if (safe_? || readPermission_?) i_is_!
    else i_obscure_!(i_is_!)
  }

  /**
   * What value was the field's value when it was pulled from the DB?
   */
  def was: FieldType = {
    if (safe_? || readPermission_?) i_was_!
    else i_obscure_!(i_was_!)
  }

  /**
   * The actual value of the field
   */
  protected def i_is_! : FieldType

  /**
   * The value of the field when it was pulled from the DB
   */
  protected def i_was_! : FieldType

  /**
   * Obscure the incoming value to a "safe" value (e.g., if there are
   * not enough rights to view the entire social security number 123-45-5678, this
   * method might return ***-**-*678
   */
  protected def i_obscure_!(in : FieldType): FieldType

  /**
   * Return the field name and field value, delimited by an '='
   */
  def asString = displayName + "=" + toString

  def dbColumnCount = 1

  def dbColumnNames(in : String) = if (dbColumnCount == 1) List(dbColumnName) else List(in.toLowerCase)

  def dbColumnName = name.toLowerCase match {
    case name if DB.reservedWords.contains(name) => name+"_c"
    case name => name
  }

  lazy val dbSelectString = fieldOwner.getSingleton.
  dbTableName + "." + dbColumnName


  def dbIndexed_? : Boolean = false

  def dbPrimaryKey_? : Boolean = false

  /**
   * Is the field a foreign key reference
   */
  def dbForeignKey_? : Boolean = false

  def jdbcFriendly(field : String) : Object

  def jdbcFriendly: Object = jdbcFriendly(dbColumnName)

  /**
   * Get the JDBC SQL Type for this field
   */
  def targetSQLType(field : String): Int = targetSQLType

  /**
   * Get the JDBC SQL Type for this field
   */
  def targetSQLType: Int

  override def toString : String =
  is match {
    case null => ""
    case v => v.toString
  }

  def validations: List[FieldType => List[FieldError]] = Nil

  def validate : List[FieldError] = {
    val cv = is
    validations.flatMap{
      case pf: PartialFunction[FieldType, List[FieldError]] =>
        if (pf.isDefinedAt(cv)) pf(cv)
        else Nil
      case f => f(cv)
    }
  }

  final def convertToJDBCFriendly(value: FieldType): Object = real_convertToJDBCFriendly(runFilters(value, setFilter))

  protected def real_convertToJDBCFriendly(value: FieldType): Object

  /**
   * Does the "right thing" comparing mapped fields
   */
  override def equals(other: Any): Boolean = {
    other match {
      case mapped: MappedField[Any, Nothing] => this.is == mapped.is
      case ov: AnyRef if (ov ne null) && dbFieldClass.isAssignableFrom(ov.getClass) => this.is == runFilters(ov.asInstanceOf[FieldType], setFilter)
      case ov => this.is == ov
    }
  }

  override def asHtml : Node = Text(toString)
}

object MappedField {
  implicit def mapToType[T, A<:Mapper[A]](in : MappedField[T, A]): T = in.is
}

trait IndexedField[O] extends BaseIndexedField {
  def convertKey(in: String): Box[O]
  def convertKey(in: Int): Box[O]
  def convertKey(in: Long): Box[O]
  def convertKey(in: AnyRef): Box[O]
  def makeKeyJDBCFriendly(in: O): AnyRef
  def dbDisplay_? = false
}

trait BaseIndexedField extends BaseMappedField {

}

/**
 * A trait that defines foreign key references
 */
trait BaseForeignKey extends BaseMappedField {

  type KeyType
  type KeyedForeignType <: KeyedMapper[KeyType, KeyedForeignType]

  type OwnerType <: Mapper[OwnerType]

  /**
   * Is the key defined?
   */
  def defined_? : Boolean

  /**
   * get the object referred to by this foreign key
   */

  def dbKeyToTable: BaseMetaMapper

  def dbKeyToColumn: BaseMappedField

  def findFor(key: KeyType): List[OwnerType]

  def findFor(key: KeyedForeignType): List[OwnerType]

  /**
   * Called when Schemifier adds a foreign key.  Return a function that will be called when Schemifier
   * is done with the schemification.
   */
  def dbAddedForeignKey: Box[() => Unit]
}

trait LifecycleCallbacks {
  def beforeValidation {}
  def beforeValidationOnCreate {}
  def beforeValidationOnUpdate {}
  def afterValidation {}
  def afterValidationOnCreate {}
  def afterValidationOnUpdate {}

  def beforeSave {}
  def beforeCreate {}
  def beforeUpdate {}

  def afterSave {}
  def afterCreate {}
  def afterUpdate {}

  def beforeDelete {}
  def afterDelete {}
}


