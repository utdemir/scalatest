/*
 * Copyright 2001-2013 Artima, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scalautils

import scala.collection.immutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.GenTraversableOnce
import scala.reflect.ClassTag
import scala.collection.mutable.Buffer
import scala.collection.GenSeq
import scala.collection.GenIterable
import scala.collection.generic.CanBuildFrom
import Every.fromNonEmptyVector
import scala.annotation.unchecked.{ uncheckedVariance => uV }

// Can't be an IndexedSeq[T] because Builder would be able to create an empty one.
/**
 * An ordered, immutable, non-empty collection of elements.
 *
 * <p>
 * Class <code>Every</code> has two and only two subtypes: <a href="One.html"><code>One</code></a> and <a href="Many.html"><code>Many</code></a>.
 * A <code>One</code> contains exactly one element. A <code>Many</code> contains two or more elements. Thus no way exists for an
 * <code>Every</code> to contain zero elements.
 * </p>
 *
 * <h2>Constructing <code>Every</code>s</h2>
 *
 * <p>
 * You can construct an <code>Every</code> by passing one or more elements to the <code>Every.apply</code> factory method:
 * </p>
 *
 * <pre class="stHighlight">
 * Every(1)
 * Every(1, 2)
 * Every(1, 2, 3)
 * </pre>
 *
 * <p>
 * Alternatively you can pass one element to the <code>One.apply</code> factory method, or two or more elements to 
 * <code>Many.apply</code>:
 * </p>
 *
 * <pre class="stHighlight">
 * One(1)
 * Many(1, 3)
 * Many(1, 2, 3)
 * </pre>
 *
 * <h2>Working with <code>Every</code>s</h2>
 *
 * <p>
 * <code>Every</code> does not extend Scala's <code>Seq</code> or <code>Traversable</code> traits because these require that
 * implementations may be empty. For example, if you invoke <code>tail</code> on a <code>Seq</code> that contains just one element,
 * you'll get an empty <code>Seq</code>:
 * </p>
 * 
 * <pre class="stREPL">
 * scala&gt; List(1).tail
 * res6: List[Int] = List()
 * </pre>
 *
 * <p>
 * On the other hand, many useful methods exist on <code>Seq</code> that when invoked on a non-empty <code>Seq</code> are guaranteed
 * to not result in an empty <code>Seq</code>. For convenience, <code>Every</code> defines a method corresponding to every such <code>Seq</code>
 * method. Here are some examples:
 * </p>
 * 
 * <pre class="stHighlight">
 * Many(1, 2, 3).map(_ + 1) // Result: Many(2, 3, 4)
 * One(1).map(_ + 1)        // Result: One(2)
 * </pre>
 *
 * <p>
 * <code>Every</code> does <em>not</em> currently define any methods corresponding to <code>Seq</code> methods that could result in
 * an empty <code>Seq</code>. However, an implicit converison from <code>Every</code> to <code>collection.immutable.IndexedSeq</code>
 * is defined in the <code>Every</code> companion object that will be applied if you attempt to call one of the missing methods. As a
 * result, you can invoke <code>filter</code> on an <code>Every</code>, even though <code>filter</code> could result
 * in an empty sequence&mdash;but the result type will be <code>collection.immutable.IndexedSeq</code> instead of <code>Every</code>:
 * </p>
 *
 * <pre class="stHighlight">
 * Every(1, 2, 3).filter(_ &lt; 10) // Result: Vector(1, 2, 3)
 * Every(1, 2, 3).filter(_ &gt; 10) // Result: Vector()
 * </pre>
 * 
 *
 * <p>
 * You can use <code>Every</code>s in <code>for</code> expressions. The result will be an <code>Every</code> unless
 * you use a filter (an <code>if</code> clause). Because filters are desugared to invocations of <code>filter</code>, the
 * result type will switch to a <code>collection.immutable.IndexedSeq</code> at that point. Here are some examples:
 * </p>
 * 
 * <pre class="stREPL">
 * scala&gt; import org.scalautils._
 * import org.scalautils._
 *
 * scala&gt; for (i &lt;- Every(1, 2, 3)) yield i + 1
 * res0: org.scalautils.Every[Int] = Many(2, 3, 4)
 *
 * scala&gt; for (i &lt;- Every(1, 2, 3) if i &lt; 10) yield i + 1
 * res1: scala.collection.immutable.IndexedSeq[Int] = Vector(2, 3, 4)
 *
 * scala&gt; for {
 *      |   i &lt;- Every(1, 2, 3)
 *      |   j &lt;- Every('a', 'b', 'c')
 *      | } yield (i, j)
 * res3: org.scalautils.Every[(Int, Char)] =
 *         Many((1,a), (1,b), (1,c), (2,a), (2,b), (2,c), (3,a), (3,b), (3,c))
 *
 * scala&gt; for {
 *      |   i &lt;- Every(1, 2, 3) if i &lt; 10
 *      |   j &lt;- Every('a', 'b', 'c')
 *      | } yield (i, j)
 * res6: scala.collection.immutable.IndexedSeq[(Int, Char)] =
 *         Vector((1,a), (1,b), (1,c), (2,a), (2,b), (2,c), (3,a), (3,b), (3,c))
 * </pre>
 *
 * <h2>Motivation for <code>Every</code>s</h2>
 *
 * <p>
 * Although <code>Every</code> is a general-purpose, non-empty ordered collection, it was motivated by the desire to enable easy
 * accumulation of errors in <a href="Or.html"><code>Or</code></a>s. For examples of <code>Every</code> used in that use case, see the
 * <a href="Or.html#accumulatingErrors">Accumulating errors with <code>Or</code></a> section in the main documentation for <code>Or</code>.
 * </p>
 *
 * @tparam T the type of elements contained in this <code>Every</code>
 */
sealed abstract class Every[+T] protected (underlying: Vector[T]) extends PartialFunction[Int, T] {

  /**
   * Returns a new <code>Many</code> containing the elements of this <code>Every</code> followed by the elements of the passed <code>Every</code>.
   * The element type of the resulting <code>Many</code> is the most specific superclass encompassing the element types of this and the passed <code>Every</code>.
   *
   * @tparam U the element type of the returned <code>Many</code>
   * @param other the <code>Every</code> to append
   * @return a new <code>Many</code> that contains all the elements of this <code>Every</code> followed by all elements of <code>other</code>.
   */
  def ++[U >: T](other: Every[U]): Many[U]

  /**
   * Returns a new <code>Many</code> containing the elements of this <code>Every</code> followed by the elements of the passed <code>GenTraversableOnce</code>.
   * The element type of the resulting <code>Many</code> is the most specific superclass encompassing the element types of this <code>Every</code>
   * and the passed <code>GenTraversableOnce</code>.
   *
   * @tparam U the element type of the returned <code>Many</code>
   * @param other the <code>Every</code> to append
   * @return a new <code>Many</code> that contains all the elements of this <code>Every</code> followed by all elements of <code>other</code>.
   */
  def ++[U >: T](other: GenTraversableOnce[U]): Every[U]

  /**
   * Fold left: applies a binary operator to a start value, <code>z</code>, and all elements of this <code>Every</code>, going left to right.
   *
   * <p>
   * Note: <code>/:</code> is alternate syntax for the <code>foldLeft</code> method; <code>z</code> <code>/:</code> <code>every</code> is the
   * same as <code>every</code> <code>foldLeft</code> <code>z</code>.
   * </p>
   *
   * @tparam B the result of the binary operator
   * @param z the start value
   * @param op the binary operator
   * @return the result of inserting <code>op</code> between consecutive elements of this <code>Every</code>, going left to right, with the start value,
   *     <code>z</code>, on the left:
   *
   * <pre>
   * op(...op(op(z, x_1), x_2), ..., x_n)
   * </pre>
   *
   * <p>
   * where x<sub>1</sub>, ..., x<sub>n</sub> are the elements of this <code>Every</code>. 
   * </p>
   */
  final def /:[B](z: B)(op: (B, T) => B): B = underlying./:(z)(op)

  /**
   * Fold right: applies a binary operator to all elements of this <code>Every</code> and a start value, going right to left.
   *
   * <p>
   * Note: <code>:\</code> is alternate syntax for the <code>foldRight</code> method; <code>every</code> <code>:\</code> <code>z</code> is the same
   * as <code>every</code> <code>foldRight</code> <code>z</code>.
   * </p>
   *
   * @tparam B the result of the binary operator
   * @param z the start value
   * @param op the binary operator
   * @return the result of inserting <code>op</code> between consecutive elements of this <code>Every</code>, going right to left, with the start value,
   *     <code>z</code>, on the right:
   *
   * <pre>
   * op(x_1, op(x_2, ... op(x_n, z)...))
   * </pre>
   *
   * <p>
   * where x<sub>1</sub>, ..., x<sub>n</sub> are the elements of this <code>Every</code>. 
   * </p>
   */
  final def :\[B](z: B)(op: (T, B) => B): B = underlying.:\(z)(op)

  /**
   * Returns a new <code>Every</code> with the given element prepended.
   *
   * <p>
   * Note that :-ending operators are right associative. A mnemonic for <code>+:</code> <em>vs.</em> <code>:+</code> is: the COLon goes on the COLlection side.
   * </p>
   *
   * @param element the element to prepend to this <code>Every</code>
   * @return a new <code>Every</code> consisting of <code>element</code> followed by all elements of this <code>Every</code>.
   */
  final def +:[U >: T](element: U): Many[U] = Many(element, underlying.head, underlying.tail: _*)

  /**
   * Returns a new <code>Every</code> with the given element appended.
   *
   * <p>
   * Note a mnemonic for <code>+:</code> <em>vs.</em> <code>:+</code> is: the COLon goes on the COLlection side.
   * </p>
   *
   * @param element the element to append to this <code>Every</code>
   * @return a new <code>Every</code> consisting of all elements of this <code>Every</code> followed by <code>element</code>.
   */
  def :+[U >: T](element: U): Many[U]

  /**
   * Appends all elements of this <code>Every</code> to a string builder. The written text will consist of a concatenation of the result of invoking <code>toString</code>
   * on of every element of this <code>Every</code>, without any separator string.
   *
   * @param sb the string builder to which elements will be appended
   * @return the string builder, <code>sb</code>, to which elements were appended.
   */
  final def addString(sb: StringBuilder): StringBuilder = underlying.addString(sb)

  /**
   * Appends all elements of this <code>Every</code> to a string builder using a separator string. The written text will consist of a concatenation of the
   * result of invoking <code>toString</code>
   * on of every element of this <code>Every</code>, separated by the string <code>sep</code>.
   *
   * @param sb the string builder to which elements will be appended
   * @param sep the separator string
   * @return the string builder, <code>sb</code>, to which elements were appended.
   */
  final def addString(sb: StringBuilder, sep: String): StringBuilder = underlying.addString(sb, sep)

  /**
   * Appends all elements of this <code>Every</code> to a string builder using start, end, and separator strings. The written text will consist of a concatenation of
   * the string <code>start</code>; the result of invoking <code>toString</code> on all elements of this <code>Every</code>,
   * separated by the string <code>sep</code>; and the string <code>end</code>
   *
   * @param sb the string builder to which elements will be appended
   * @param start the starting string
   * @param sep the separator string
   * @param start the ending string
   * @return the string builder, <code>sb</code>, to which elements were appended.
   */
  final def addString(sb: StringBuilder, start: String, sep: String, end: String): StringBuilder = underlying.addString(sb, start, sep, end)

  /**
   * Selects an element by its index in the <code>Every</code>.
   *
   * @return the element of this <code>Every</code> at index <code>idx</code>, where 0 indicates the first element.
   */
  final def apply(idx: Int): T = underlying(idx)

  /**
   * Finds the first element of the <code>Every</code> for which the given partial function is defined, if any, and applies the partial function to it.
   *
   * @param pf the partial function
   * @return an <code>Option</code> containing <code>pf</code> applied to the first element for which it is defined, or <code>None</code> if
   *    the partial function was not defined for any element.
   */
  final def collectFirst[U](pf: PartialFunction[T, U]): Option[U] = underlying.collectFirst(pf)

  /**
   * Indicates whether this <code>Every</code> contains a given value as an element.
   *
   * @param elem the element to look for
   * @return true if this <code>Every</code> has an element that is equal (as determined by <code>==)</code> to <code>elem</code>, false otherwise. 
   */ 
  final def contains(elem: Any): Boolean = underlying.contains(elem)

  /**
   * Indicates whether this <code>Every</code> contains a given <code>GenSeq</code> as a slice.
   *
   * @param that the <code>GenSeq</code> slice to look for
   * @return true if this <code>Every</code> contains a slice with the same elements as <code>that</code>, otherwise <code>false</code>.
   */
  final def containsSlice[B](that: GenSeq[B]): Boolean = underlying.containsSlice(that)

  /**
   * Indicates whether this <code>Every</code> contains a given <code>Every</code> as a slice.
   *
   * @param that the <code>Every</code> slice to look for
   * @return true if this <code>Every</code> contains a slice with the same elements as <code>that</code>, otherwise <code>false</code>.
   */
  final def containsSlice[B](that: Every[B]): Boolean = underlying.containsSlice(that.toVector)

  /**
   * Copies values of this <code>Every</code> to an array. Fills the given array <code>arr</code> with values of this <code>Every</code>. Copying
   * will stop once either the end of the current <code>Every</code> is reached, or the end of the array is reached.
   *
   * @param arr the array to fill
   */
  final def copyToArray[U >: T](arr: Array[U]): Unit = underlying.copyToArray(arr)

  /**
   * Copies values of this <code>Every</code> to an array. Fills the given array <code>arr</code> with values of this <code>Every</code>, beginning at
   * index <code>start</code>. Copying will stop once either the end of the current <code>Every</code> is reached, or the end of the array is reached.
   *
   * @param arr the array to fill
   * @param start the starting index
   */
  final def copyToArray[U >: T](arr: Array[U], start: Int): Unit = underlying.copyToArray(arr, start)

  /**
   * Copies values of this <code>Every</code> to an array. Fills the given array <code>arr</code> with at most <code>len</code> elements of this <code>Every</code>, beginning at
   * index <code>start</code>. Copying will stop once either the end of the current <code>Every</code> is reached, the end of the array is reached, or
   * <code>len</code> elements have been copied.
   *
   * @param arr the array to fill
   * @param start the starting index
   * @param len the maximum number of elements to copy
   */
  final def copyToArray[U >: T](arr: Array[U], start: Int, len: Int): Unit = underlying.copyToArray(arr, start, len)

  /**
   * Copies all elements of this <code>Every</code> to a buffer. 
   *
   * @param buf the buffer to which elements are copied
   */
  final def copyToBuffer[U >: T](buf: Buffer[U]): Unit = underlying.copyToBuffer(buf)

  /**
   * Indicates whether every element of this <code>Every</code> relates to the corresponding element of a given <code>GenSeq</code> by satisfying a given predicate. 
   *
   * @tparam B the type of the elements of <code>that</code>
   * @param that the <code>GenSeq</code> to compare for correspondence
   * @param p the predicate, which relates elements from this <code>Every</code> and the passed <code>GenSeq</code>
   * @return true if this <code>Every</code> and the passed <code>GenSeq</code> have the same length and <code>p(x, y)</code> is <code>true</code>
   *     for all corresponding elements <code>x</code> of this <code>Every</code> and <code>y</code> of that, otherwise <code>false</code>.
   */
  final def corresponds[B](that: GenSeq[B])(p: (T, B) => Boolean): Boolean = underlying.corresponds(that)(p)

  /**
   * Indicates whether every element of this <code>Every</code> relates to the corresponding element of a given <code>Every</code> by satisfying a given predicate. 
   *
   * @tparam B the type of the elements of <code>that</code>
   * @param that the <code>Every</code> to compare for correspondence
   * @param p the predicate, which relates elements from this and the passed <code>Every</code>
   * @return true if this and the passed <code>Every</code> have the same length and <code>p(x, y)</code> is <code>true</code>
   *     for all corresponding elements <code>x</code> of this <code>Every</code> and <code>y</code> of that, otherwise <code>false</code>.
   */
  final def corresponds[B](that: Every[B])(p: (T, B) => Boolean): Boolean = underlying.corresponds(that.toVector)(p)

  /**
   * Counts the number of elements in the <code>Every</code> that satisfy a predicate. 
   *
   * @param p the predicate used to test elements.
   * @return the number of elements satisfying the predicate <code>p</code>. 
   */
  final def count(p: T => Boolean): Int = underlying.count(p)

  /**
   * Builds a new <code>Every</code> from this <code>Every</code> without any duplicate elements.
   *
   * @return A new <code>Every</code> that contains the first occurrence of every element of this <code>Every</code>. 
   */
  final def distinct: Every[T] = {
    val eles = underlying.distinct
    val head = eles.head
    val tail = eles.tail
    if (tail.isEmpty) One(head) else Many(head, tail.head, tail.tail: _*)
  }

  /**
   * Indicates whether this <code>Every</code> ends with the given <code>GenSeq</code>.
   *
   * @param the sequence to test
   * @return <code>true</code> if this <code>Every</code> has <code>that</code> as a suffix, <code>false</code> otherwise. 
   */
  final def endsWith[B](that: GenSeq[B]): Boolean = underlying.endsWith(that)

  /**
   * Indicates whether this <code>Every</code> ends with the given <code>Every</code>.
   *
   * @param the sequence to test
   * @return <code>true</code> if this <code>Every</code> has <code>that</code> as a suffix, <code>false</code> otherwise. 
   */
  final def endsWith[B](that: Every[B]): Boolean = underlying.endsWith(that.toVector)

  /**
   * Indicates whether a predicate holds for at least one of the elements of this <code>Every</code>.
   *
   * @param the predicate used to test elements.
   * @return <code>true</code> if the given predicate <code>p</code> holds for some of the elements of this <code>Every</code>, otherwise <code>false</code>. 
   */
  final def exists(p: T => Boolean): Boolean = underlying.exists(p)

  /**
   * Finds the first element of this <code>Every</code> that satisfies the given predicate, if any.
   *
   * @param p the predicate used to test elements
   * @return an <code>Some</code> containing the first element in this <code>Every</code> that satisfies <code>p</code>, or <code>None</code> if none exists. 
   */
  final def find(p: T => Boolean): Option[T] = underlying.find(p)

  /**
   * Builds a new <code>Every</code> by applying a function to all elements of this <code>Every</code> and using the elements of the resulting <code>Every</code>s.
   *
   * @tparam U the element type of the returned <code>Every</code>
   * @param f the function to apply to each element.
   * @return a new <code>Every</code> containing elements obtained by applying the given function <code>f</code> to each element of this <code>Every</code> and concatenating
   *    the elements of resulting <code>Every</code>s. 
   */
  final def flatMap[U](f: T => Every[U]): Every[U] = {
    val buf = new ArrayBuffer[U]
    for (ele <- underlying)
      buf ++= f(ele).toVector
    val vec = buf.toVector
    Every(vec.head, vec.tail: _*)
  }

  /**
   * Converts this <code>Every</code> of <code>Every</code>s into an <code>Every</code>
   * formed by the elements of the nested <code>Every</code>s.
   *
   * <p>
   * Note: You cannot use this <code>flatten</code> method on an <code>Every</code> that contains a <code>GenTraversableOnce</code>s, because 
   * if all the nested <code>GenTraversableOnce</code>s were empty, you'd end up with an empty <code>Every</code>.
   * </p>
   *
   * @tparm B the type of the elements of each nested <code>Every</code>
   * @return a new <code>Every</code> resulting from concatenating all nested <code>Every</code>s.
   */
  final def flatten[B](implicit ev: T <:< Every[B]): Every[B] = flatMap(ev)

  /**
   * Folds the elements of this <code>Every</code> using the specified associative binary operator.
   *
   * <p>
   * The order in which operations are performed on elements is unspecified and may be nondeterministic. 
   * </p>
   *
   * @tparam U a type parameter for the binary operator, a supertype of T.
   * @param z a neutral element for the fold operation; may be added to the result an arbitrary number of
   *     times, and must not change the result (<em>e.g.</em>, <code>Nil</code> for list concatenation,
   *     0 for addition, or 1 for multiplication.)
   * @param op a binary operator that must be associative
   * @return the result of applying fold operator <code>op</code> between all the elements and <code>z</code>
   */
  final def fold[U >: T](z: U)(op: (U, U) => U): U = underlying.fold(z)(op)

  /**
   * Applies a binary operator to a start value and all elements of this vector, going left to right.
   *
   * @tparam B the result type of the binary operator.
   * @param z the start value.
   * @param op the binary operator.
   * @return the result of inserting <code>op</code> between consecutive elements of this <code>Every</code>, going left to right, with the start value,
   *     <code>z</code>, on the left:
   *
   * <pre>
   * op(...op(op(z, x_1), x_2), ..., x_n)
   * </pre>
   *
   * <p>
   * where x<sub>1</sub>, ..., x<sub>n</sub> are the elements of this <code>Every</code>. 
   * </p>
   */ 
  final def foldLeft[B](z: B)(op: (B, T) => B): B = underlying.foldLeft(z)(op)

  /**
   * Applies a binary operator to all elements of this <code>Every</code> and a start value, going right to left.
   *
   * @tparam B the result of the binary operator
   * @param z the start value
   * @param op the binary operator
   * @return the result of inserting <code>op</code> between consecutive elements of this <code>Every</code>, going right to left, with the start value,
   *     <code>z</code>, on the right:
   *
   * <pre>
   * op(x_1, op(x_2, ... op(x_n, z)...))
   * </pre>
   *
   * <p>
   * where x<sub>1</sub>, ..., x<sub>n</sub> are the elements of this <code>Every</code>. 
   * </p>
   */
  final def foldRight[B](z: B)(op: (T, B) => B): B = underlying.foldRight(z)(op)

  /**
   * Indicates whether a predicate holds for all elements of this <code>Every</code>.
   *
   * @param p the predicate used to test elements.
   * @return <code>true</code> if the given predicate <code>p</code> holds for all elements of this <code>Every</code>, otherwise <code>false</code>. 
   */
  final def forall(p: T => Boolean): Boolean = underlying.forall(p)

  /**
   * Applies a function <code>f</code> to all elements of this <code>Every</code>.
   *
   * @param f the function that is applied for its side-effect to every element. The result of function <code>f</code> is discarded.
   */ 
  final def foreach(f: T => Unit): Unit = underlying.foreach(f)

  /**
   * Partitions this <code>Every</code> into a map of <code>Every</code>s according to some discriminator function.
   *
   * @tparam K the type of keys returned by the discriminator function.
   * @param f the discriminator function.
   * @return A map from keys to <code>Every</code>s such that the following invariant holds:
   *
   * <pre>
   * (every.toVector partition f)(k) = xs filter (x =&gt; f(x) == k)
   * </pre>
   *
   * <p>
   * That is, every key <code>k</code> is bound to an <code>Every</code> of those elements <code>x</code> for which <code>f(x)</code> equals <code>k</code>.
   * </p>
   */
  final def groupBy[K](f: T => K): Map[K, Every[T]] = {
    val mapKToVec = underlying.groupBy(f)
    mapKToVec.mapValues { vec => Every(vec.head, vec.tail: _*) }
  }

  /**
   * Partitions elements into fixed size <code>Every</code>s.
   *
   * @param size the number of elements per group
   * @return An iterator producing <code>Every</code>s of size <code>size</code>, except the last will be truncated if the elements don't divide evenly. 
   */
  final def grouped(size: Int): Iterator[Every[T]] = {
    val itOfVec = underlying.grouped(size)
    itOfVec.map { vec => Every(vec.head, vec.tail: _*) }
  }

  /**
   * Returns <code>true</code> to indicate this <code>Every</code> has a definite size, since all <code>Every</code>s are strict collections.
   */
  final def hasDefiniteSize: Boolean = true

  /**
   * Selects the first element of this <code>Every</code>. 
   *
   * @return the first element of this <code>Every</code>.
   */
  final def head: T = underlying.head

  // Methods like headOption I can't get rid of because of the implicit conversion to GenTraversable.
  // Users can call any of the methods I've left out on an Every, and get whatever Vector would return
  // for that method call. Eventually I'll probably implement them all to save the implicit conversion.

  /**
   * Selects the first element of this <code>Every</code> and returns it wrapped in a <code>Some</code>. 
   *
   * @return the first element of this <code>Every</code>, wrapped in a <code>Some</code>.
   */
  final def headOption: Option[T] = underlying.headOption

  /**
   * Finds index of first occurrence of some value in this <code>Every</code>.
   *
   * @param elem the element value to search for. 
   * @return the index of the first element of this <code>Every</code> that is equal (as determined by <code>==</code>) to <code>elem</code>,
   *     or <code>-1</code>, if none exists.
   */
  final def indexOf[U >: T](elem: U): Int = underlying.indexOf(elem)

  /**
   * Finds index of first occurrence of some value in this <code>Every</code> after or at some start index.
   *
   * @param elem the element value to search for. 
   * @param from the start index
   * @return the index <code>&gt;=</code> <code>from</code> of the first element of this vector that is equal (as determined by <code>==</code>) to <code>elem</code>,
   *     or <code>-1</code>, if none exists.
   */
  final def indexOf[U >: T](elem: U, from: Int): Int = underlying.indexOf(elem, from)

  /**
   * Finds first index where this <code>Every</code> contains a given <code>GenSeq</code> as a slice.
   * 
   * @param that the <code>GenSeq<code> defining the slice to look for
   * @return the first index at which the elements of this <code>Every</code> starting at that index match the elements of
   *     <code>GenSeq</code> <code>that</code>, or <code>-1</code> of no such subsequence exists. 
   */
  final def indexOfSlice[U >: T](that: GenSeq[U]): Int = underlying.indexOfSlice(that)

  /**
   * Finds first index after or at a start index where this <code>Every</code> contains a given <code>GenSeq</code> as a slice.
   * 
   * @param that the <code>GenSeq<code> defining the slice to look for
   * @param from the start index
   * @return the first index <code>&gt;=</code> <code>from</code> at which the elements of this <code>Every</code> starting at that index match the elements of
   *     <code>GenSeq</code> <code>that</code>, or <code>-1</code> of no such subsequence exists. 
   */
  final def indexOfSlice[U >: T](that: GenSeq[U], from: Int): Int = underlying.indexOfSlice(that, from)

  /**
   * Finds first index where this <code>Every</code> contains a given <code>Every</code> as a slice.
   * 
   * @param that the <code>Every<code> defining the slice to look for
   * @return the first index such that the elements of this <code>Every</code> starting at this index match the elements of
   *     <code>Every</code> <code>that</code>, or <code>-1</code> of no such subsequence exists. 
   */
  final def indexOfSlice[U >: T](that: Every[U]): Int = underlying.indexOfSlice(that.toVector)

  /**
   * Finds first index after or at a start index where this <code>Every</code> contains a given <code>Every</code> as a slice.
   * 
   * @param that the <code>Every<code> defining the slice to look for
   * @param from the start index
   * @return the first index <code>&gt;=</code> <code>from</code> such that the elements of this <code>Every</code> starting at this index match the elements of
   *     <code>Every</code> <code>that</code>, or <code>-1</code> of no such subsequence exists. 
   */
  final def indexOfSlice[U >: T](that: Every[U], from: Int): Int = underlying.indexOfSlice(that.toVector, from)

  /**
   * Finds index of the first element satisfying some predicate.
   *
   * @param p the predicate used to test elements.
   * @return the index of the first element of this <code>Every</code> that satisfies the predicate <code>p</code>,
   *     or <code>-1</code>, if none exists.
   */ 
  final def indexWhere(p: T => Boolean): Int = underlying.indexWhere(p)

  /**
   * Finds index of the first element satisfying some predicate after or at some start index.
   *
   * @param p the predicate used to test elements.
   * @param from the start index
   * @return the index <code>&gt;=</code> <code>from</code> of the first element of this <code>Every</code> that satisfies the predicate <code>p</code>,
   *     or <code>-1</code>, if none exists.
   */ 
  final def indexWhere(p: T => Boolean, from: Int): Int = underlying.indexWhere(p, from)

  /**
   * Produces the range of all indices of this <code>Every</code>. 
   *
   * @return a <code>Range</code> value from <code>0</code> to one less than the length of this <code>Every</code>. 
   */
  final def indices: Range = underlying.indices

  /**
   * Tests whether this <code>Every</code> contains given index.
   *
   * @param idx the index to test
   * @return true if this <code>Every</code> contains an element at position <code>idx</code>, <code>false</code> otherwise. 
   */
  final def isDefinedAt(idx: Int): Boolean = underlying.isDefinedAt(idx)

  /**
   * Returns <code>false</code> to indicate this <code>Every</code>, like all <code>Every<code>s, is non-empty.
   *
   * @return false
   */
  final def isEmpty: Boolean = false

  /**
   * Returns <code>true</code> to indicate this <code>Every</code>, like all <code>Every<code>s, can be traversed repeatedly.
   *
   * @return true
   */
  final def isTraversableAgain: Boolean = true

  /**
   * Creates and returns a new iterator over all elements contained in this <code>Every</code>.
   *
   * @return the new iterator
   */
  final def iterator: Iterator[T] = underlying.iterator

  /**
   * Selects the last element of this <code>Every</code>. 
   *
   * @return the last element of this <code>Every</code>.
   */
  final def last: T = underlying.last

  /**
   * Finds the index of the last occurrence of some value in this <code>Every</code>.
   *
   * @param elem the element value to search for.
   * @return the index of the last element of this <code>Every</code> that is equal (as determined by <code>==</code>) to <code>elem</code>,
   *     or <code>-1</code>, if none exists.
   */
  final def lastIndexOf[U >: T](elem: U): Int = underlying.lastIndexOf(elem)

  /**
   * Finds the index of the last occurrence of some value in this <code>Every</code> before or at a given <code>end</code> index.
   *
   * @param elem the element value to search for.
   * @param end the end index. 
   * @return the index <code>&gt;=</code> <code>end</code> of the last element of this <code>Every</code> that is equal (as determined by <code>==</code>)
   *     to <code>elem</code>, or <code>-1</code>, if none exists.
   */
  final def lastIndexOf[U >: T](elem: U, end: Int): Int = underlying.lastIndexOf(elem, end)

  /**
   * Finds the last index where this <code>Every</code> contains a given <code>GenSeq</code> as a slice. 
   *
   * @param that the <code>GenSeq</code> defining the slice to look for
   * @return the last index at which the elements of this <code>Every</code> starting at that index match the elements of
   *    <code>GenSeq</code> <code>that</code>, or <code>-1</code> of no such subsequence exists. 
   */
  final def lastIndexOfSlice[U >: T](that: GenSeq[U]): Int = underlying.lastIndexOfSlice(that)

  /**
   * Finds the last index before or at a given end index where this <code>Every</code> contains a given <code>GenSeq</code> as a slice. 
   *
   * @param that the <code>GenSeq</code> defining the slice to look for
   * @param end the end index
   * @return the last index <code>&gt;=</code> <code>end</code> at which the elements of this <code>Every</code> starting at that index match the elements of
   *    <code>GenSeq</code> <code>that</code>, or <code>-1</code> of no such subsequence exists. 
   */
  final def lastIndexOfSlice[U >: T](that: GenSeq[U], end: Int): Int = underlying.lastIndexOfSlice(that, end)

  /**
   * Finds the last index where this <code>Every</code> contains a given <code>Every</code> as a slice. 
   *
   * @param that the <code>Every</code> defining the slice to look for
   * @return the last index at which the elements of this <code>Every</code> starting at that index match the elements of
   *    <code>Every</code> <code>that</code>, or <code>-1</code> of no such subsequence exists. 
   */
  final def lastIndexOfSlice[U >: T](that: Every[U]): Int = underlying.lastIndexOfSlice(that.toVector)

  /**
   * Finds the last index before or at a given end index where this <code>Every</code> contains a given <code>Every</code> as a slice. 
   *
   * @param that the <code>Every</code> defining the slice to look for
   * @param end the end index
   * @return the last index <code>&gt;=</code> <code>end</code> at which the elements of this <code>Every</code> starting at that index match the elements of
   *    <code>Every</code> <code>that</code>, or <code>-1</code> of no such subsequence exists. 
   */
  final def lastIndexOfSlice[U >: T](that: Every[U], end: Int): Int = underlying.lastIndexOfSlice(that.toVector, end)

  /**
   * Finds index of last element satisfying some predicate.
   *
   * @param p the predicate used to test elements.
   * @return the index of the last element of this <code>Every</code> that satisfies the predicate <code>p</code>, or <code>-1</code>, if none exists. 
   */
  final def lastIndexWhere(p: T => Boolean): Int = underlying.lastIndexWhere(p)

  /**
   * Finds index of last element satisfying some predicate before or at given end index.
   *
   * @param p the predicate used to test elements.
   * @param end the end index
   * @return the index <code>&gt;=</code> <code>end</code> of the last element of this <code>Every</code> that satisfies the predicate <code>p</code>,
   *     or <code>-1</code>, if none exists. 
   */
  final def lastIndexWhere(p: T => Boolean, end: Int): Int = underlying.lastIndexWhere(p, end)

  /**
   * Returns the last element of this <code>Every</code>, wrapped in a <code>Some</code>. 
   *
   * @return the last element, wrapped in a <code>Some</code>. 
   */
  final def lastOption: Option[T] = underlying.lastOption // Will always return a Some

  /**
   * The length of this <code>Every</code>.
   *
   * <p>
   * Note: <code>length</code> and <code>size</code> yield the same result, which will be <code>&gt;</code>= 1. 
   * </p>
   *
   * @return the number of elements in this <code>Every</code>. 
   */
  final def length: Int = underlying.length

  /**
   * Compares the length of this <code>Every</code> to a test value. 
   *
   * @param len the test value that gets compared with the length.
   * @return a value <code>x</code> where
   *
   * <pre>
   * x &lt; 0 if this.length &lt; len
   * x == 0 if this.length == len
   * x &gt; 0 if this.length &gt; len
   * </pre>
   */
  final def lengthCompare(len: Int): Int = underlying.lengthCompare(len)

  /**
   * Builds a new <code>Every</code> by applying a function to all elements of this <code>Every</code>.
   *
   * @tparam U the element type of the returned <code>Every</code>.
   * @param f the function to apply to each element. 
   * @return a new <code>Every</code> resulting from applying the given function <code>f</code> to each element of this <code>Every</code> and collecting the results. 
   */
  final def map[U](f: T => U): Every[U] = {
    val vec = underlying.map(f)
    Every(vec.head, vec.tail: _*)
  }

  /**
   * Finds the largest element.
   *
   * @return the largest element of this <code>Every</code>. 
   */
  final def max[U >: T](implicit cmp: Ordering[U]): T = underlying.max(cmp)

  /**
   * Finds the largest result after applying the given function to every element.
   *
   * @return the largest result of applying the given function to every element of this <code>Every</code>. 
   */
  final def maxBy[U](f: T => U)(implicit cmp: Ordering[U]): T = underlying.maxBy(f)(cmp)

  /**
   * Finds the smallest element.
   *
   * @return the smallest element of this <code>Every</code>. 
   */
  final def min[U >: T](implicit cmp: Ordering[U]): T = underlying.min(cmp)

  /**
   * Finds the smallest result after applying the given function to every element.
   *
   * @return the smallest result of applying the given function to every element of this <code>Every</code>. 
   */
  final def minBy[U](f: T => U)(implicit cmp: Ordering[U]): T = underlying.minBy(f)(cmp)

  /**
   * Displays all elements of this <code>Every</code> in a string. 
   *
   * @return a string representation of this <code>Every</code>. In the resulting string, the result of invoking <code>toString</code> on all elements of this
   *     <code>Every</code> follow each other without any separator string. 
   */
  final def mkString: String = underlying.mkString

  /**
   * Displays all elements of this <code>Every</code> in a string using a separator string. 
   *
   * @param sep the separator string
   * @return a string representation of this <code>Every</code>. In the resulting string, the result of invoking <code>toString</code> on all elements of this
   *     <code>Every</code> are separated by the string <code>sep</code>. 
   */
  final def mkString(sep: String): String = underlying.mkString(sep)

  /**
   * Displays all elements of this <code>Every</code> in a string using start, end, and separator strings. 
   *
   * @param start the starting string.
   * @param sep the separator string.
   * @param end the ending string.
   * @return a string representation of this <code>Every</code>. The resulting string begins with the string <code>start</code> and ends with the string
   *     <code>end</code>. Inside, In the resulting string, the result of invoking <code>toString</code> on all elements of this <code>Every</code> are
   *     separated by the string <code>sep</code>. 
   */
  final def mkString(start: String, sep: String, end: String): String = underlying.mkString(start, sep, end)

  /**
   * Returns <code>true</code> to indicate this <code>Every</code>, like all <code>Every<code>s, is non-empty.
   *
   * @return true
   */
  final def nonEmpty: Boolean = true

  /**
   * A copy of this <code>Every</code> with an element value appended until a given target length is reached.
   *
   * @param len the target length 
   * @param elem he padding value
   * @return a new <code>Every</code> consisting of all elements of this <code>Every</code> followed by the minimal number of occurrences
   *     of <code>elem</code> so that the resulting <code>Every</code> has a length of at least <code>len</code>. 
   */
  final def padTo[U >: T](len: Int, elem: U): Every[U] = {
    val vec = underlying.padTo(len, elem)
    Every(vec.head, vec.tail: _*)
  }

  /**
   * Produces a new <code>Every</code> where a slice of elements in this <code>Every</code> is replaced by another <code>Every</code>
   *
   * @param from the index of the first replaced element 
   * @param that the <code>Every</code> whose elements should replace a slice in this <code>Every</code>
   * @param replaced the number of elements to drop in the original <code>Every</code>
   */
  final def patch[U >: T](from: Int, that: Every[U], replaced: Int): Every[U] = {
    val vec = underlying.patch(from, that.toVector, replaced)
    Every(vec.head, vec.tail: _*)
  }

  /**
   * Iterates over distinct permutations. 
   *
   * <p>
   * Here's an example:
   * </p>
   *
   * <pre class="stHighlight">
   * Every('a', 'b', 'b').permutations.toList = Iterator(Many(a, b, b), Many(b, a, b), Many(b, b, a))
   * </pre>
   *
   * @return an iterator which traverses the distinct permutations of this <code>Every</code>.
   */
  final def permutations: Iterator[Every[T]] = {
    val it = underlying.permutations
    it map { v => Every(v.head, v.tail: _*) }
  }

  /**
   * Returns the length of the longest prefix whose elements all satisfy some predicate.
   *
   * @param p the predicate used to test elements.
   * @return the length of the longest prefix of this <code>Every</code> such that every element
   *     of the segment satisfies the predicate <code>p</code>. 
   */
  final def prefixLength(p: T => Boolean): Int = underlying.prefixLength(p)

  /**
   * The product of multiplying all the elements of this <code>Every</code>.
   *
   * <p>
   * This method can be invoked for any <code>Every[T]</code> for which an implicit <code>Numeric[T]</code> exists.
   * </p>
   *
   * @return the product of all elements
   */
  final def product[U >: T](implicit num: Numeric[U]): U = underlying.product(num)

  /**
   * Reduces the elements of this <code>Every</code> using the specified associative binary operator.
   *
   * <p>
   * The order in which operations are performed on elements is unspecified and may be nondeterministic. 
   * </p>
   *
   * @tparam U a type parameter for the binary operator, a supertype of T.
   * @param op a binary operator that must be associative.
   * @return the result of applying reduce operator <code>op</code> between all the elements of this <code>Every</code>.
   */
  final def reduce[U >: T](op: (U, U) => U): U = underlying.reduce(op)

  /**
   * Applies a binary operator to all elements of this vector, going left to right.
   *
   * @tparam U the result type of the binary operator.
   * @param op the binary operator.
   * @return the result of inserting <code>op</code> between consecutive elements of this <code>Every</code>, going left to right:
   *
   * <pre>
   * op(...op(op(x_1, x_2), x_3), ..., x_n)
   * </pre>
   *
   * <p>
   * where x<sub>1</sub>, ..., x<sub>n</sub> are the elements of this <code>Every</code>. 
   * </p>
   */
  final def reduceLeft[U >: T](op: (U, T) => U): U = underlying.reduceLeft(op)

  /**
   * Applies a binary operator to all elements of this vector, going left to right, returning the result in a <code>Some</code>.
   *
   * @tparam U the result type of the binary operator.
   * @param op the binary operator.
   * @return a <code>Some</code> containing the result of <code>reduceLeft(op)</code>
   * </p>
   */
  final def reduceLeftOption[U >: T](op: (U, T) => U): Option[U] = underlying.reduceLeftOption(op)

  final def reduceOption[U >: T](op: (U, U) => U): Option[U] = underlying.reduceOption(op)

  /**
   * Applies a binary operator to all elements of this <code>Every</code>, going right to left.
   *
   * @tparam U the result of the binary operator
   * @param op the binary operator
   * @return the result of inserting <code>op</code> between consecutive elements of this <code>Every</code>, going right to left:
   *
   * <pre>
   * op(x_1, op(x_2, ... op(x_{n-1}, x_n)...))
   * </pre>
   *
   * <p>
   * where x<sub>1</sub>, ..., x<sub>n</sub> are the elements of this <code>Every</code>. 
   * </p>
   */
  final def reduceRight[U >: T](op: (T, U) => U): U = underlying.reduceRight(op)

  /**
   * Applies a binary operator to all elements of this <code>Every</code>, going right to left, returning the result in a <code>Some</code>.
   *
   * @tparam U the result of the binary operator
   * @param op the binary operator
   * @return a <code>Some</code> containing the result of <code>reduceRight(op)</code>
   */
  final def reduceRightOption[U >: T](op: (T, U) => U): Option[U] = underlying.reduceRightOption(op)

  /**
   * Returns new <code>Every</code> wih elements in reverse order.
   *
   * @return a new <code>Every</code> with all elements of this <code>Every</code> in reversed order. 
   */
  final def reverse: Every[T] = {
    val vec = underlying.reverse
    Every(vec.head, vec.tail: _*)
  }

  /**
   * An iterator yielding elements in reverse order.
   *
   * <p>
   * Note: <code>every.reverseIterator</code> is the same as <code>every.reverse.iterator</code>, but might be more efficient. 
   * </p>
   *
   * @return an iterator yielding the elements of this <code>Every</code> in reversed order 
   */
  final def reverseIterator: Iterator[T] = underlying.reverseIterator

  /**
   * Builds a new <code>Every</code> by applying a function to all elements of this <code>Every</code> and collecting the results in reverse order.
   *
   * <p>
   * Note: <code>every.reverseMap(f)</code> is the same as <code>every.reverse.map(f)</code>, but might be more efficient. 
   * </p>
   *
   * @tparam U the element type of the returned <code>Every</code>.
   * @param f the function to apply to each element. 
   * @return a new <code>Every</code> resulting from applying the given function <code>f</code> to each element of this <code>Every</code>
   *     and collecting the results in reverse order. 
   */
  final def reverseMap[U](f: T => U): Every[U] = {
    val vec = underlying.reverseMap(f)
    Every(vec.head, vec.tail: _*)
  }
  final def sameElements[U >: T](that: GenIterable[U]): Boolean = underlying.sameElements(that)
  final def sameElements[U >: T](that: Every[U]): Boolean = underlying.sameElements(that.toVector)
  final def segmentLength(p: T => Boolean, from: Int): Int = underlying.segmentLength(p, from)
  final def sliding(size: Int): Iterator[Every[T]] = underlying.sliding(size).map(fromNonEmptyVector(_))
  final def sliding(size: Int, step: Int): Iterator[Every[T]] = underlying.sliding(size, step).map(fromNonEmptyVector(_))

  /**
   * The size of this <code>Every</code>.
   *
   * <p>
   * Note: <code>length</code> and <code>size</code> yield the same result, which will be <code>&gt;</code>= 1. 
   * </p>
   *
   * @return the number of elements in this <code>Every</code>. 
   */
  final def size: Int = underlying.size
  final def sortBy[U](f: T => U)(implicit ord: math.Ordering[U]): Every[T] = fromNonEmptyVector(underlying.sortBy(f))
  final def sortWith(lt: (T, T) => Boolean): Every[T] = fromNonEmptyVector(underlying.sortWith(lt))
  final def sorted[U >: T](implicit ord: math.Ordering[U]): Every[U] = fromNonEmptyVector(underlying.sorted(ord))
  final def startsWith[B](that: GenSeq[B]): Boolean = underlying.startsWith(that)
  final def startsWith[B](that: GenSeq[B], offset: Int): Boolean = underlying.startsWith(that, offset)
  final def startsWith[B](that: Every[B]): Boolean = underlying.startsWith(that.toVector)
  final def startsWith[B](that: Every[B], offset: Int): Boolean = underlying.startsWith(that.toVector, offset)
  def stringPrefix: String
  final def sum[U >: T](implicit num: Numeric[U]): U = underlying.sum(num)
  final def to[Col[_]](implicit cbf: CanBuildFrom[Nothing, T, Col[T @uV]]): Col[T @uV] = underlying.to[Col](cbf)
  final def toArray[U >: T](implicit classTag: ClassTag[U]): Array[U] = underlying.toArray
  final def toVector: Vector[T] = underlying
  final def toBuffer[U >: T]: Buffer[U] = underlying.toBuffer
  final def toIndexedSeq: IndexedSeq[T] = underlying.toIndexedSeq
  final def toIterable: Iterable[T] = underlying.toIterable
  final def toIterator: Iterator[T] = underlying.toIterator
  final def toList: List[T] = underlying.toList
  final def toMap[K, V](implicit ev: T <:< (K, V)): Map[K, V] = underlying.toMap
  final def toSeq: Seq[T] = underlying.toSeq
  final def toSet[U >: T]: Set[U] = underlying.toSet
  final def toStream: Stream[T] = underlying.toStream
  final def toTraversable: Traversable[T] = underlying.toTraversable
  final def transpose[U](implicit asTraversable: T => GenTraversableOnce[U]): Every[Every[U]] = {
    val asVecs = underlying.map(asTraversable)
    val vec = asVecs.transpose
    fromNonEmptyVector(vec map fromNonEmptyVector)
  }
  final def union[U >: T](that: Every[U]): Every[U] = fromNonEmptyVector(underlying union that.toVector)
  final def union[U >: T](that: GenSeq[U])(implicit cbf: CanBuildFrom[Vector[T], U, Vector[U]]): Every[U] = fromNonEmptyVector(underlying.union(that)(cbf))
  final def unzip[L, R](implicit asPair: T => (L, R)): (Every[L], Every[R]) = {
    val unzipped = underlying.unzip
    (fromNonEmptyVector(unzipped._1), fromNonEmptyVector(unzipped._2))
  }
  final def unzip3[L, M, R](implicit asTriple: T => (L, M, R)): (Every[L], Every[M], Every[R]) = {
    val unzipped = underlying.unzip3
    (fromNonEmptyVector(unzipped._1), fromNonEmptyVector(unzipped._2), fromNonEmptyVector(unzipped._3))
  }
  final def updated[U >: T](index: Int, elem: U): Every[U] = fromNonEmptyVector(underlying.updated(index, elem))
  final def zipAll[O, U >: T](other: collection.Iterable[O], thisElem: U, otherElem: O): Every[(U, O)] =
    Every.from(underlying.zipAll(other, thisElem, otherElem)).get
  final def zipWithIndex: Every[(T, Int)] = fromNonEmptyVector(underlying.zipWithIndex)
}

object Every {
  def apply[T](firstElement: T, otherElements: T*): Every[T] = 
    if (otherElements.isEmpty) One(firstElement) else Many(firstElement, otherElements.head, otherElements.tail: _*)
  def unapplySeq[T](every: Every[T]): Option[Seq[T]] = Some(every.toVector)
  def from[T](seq: GenSeq[T]): Option[Every[T]] =
    seq.headOption match {
      case None => None
      case Some(first) =>
        seq.tail.headOption match {
          case None => Some(One(first))
          case Some(second) => Some(Many(first, second, seq.tail.tail.seq: _*)) 
        }
    }
  // Can be flattened: Vector(Every(1, 2, 3), Every(1, 2, 3)).flatten shouldBe Vector(1, 2, 3, 1, 2, 3)
  implicit def everyToGenTraversableOnce[E](every: Every[E]): scala.collection.immutable.IndexedSeq[E] = every.toVector
  private def fromNonEmptyVector[E](vec: Vector[E]): Every[E] = Every(vec.head, vec.tail: _*)
}

/**
 * An <code>Every</code> that contains exactly one element.
 *
 * <p>
 * For more information and examples, see the main documentation for superclass <a href="Every.html"><code>Every</code></a>.
 * </p>
 *
 * @tparam T the type of the element contained in this <code>One</code>
 * @param loneElement the lone element contained in this <code>One</code>
 */
final case class One[+T](loneElement: T) extends Every[T](Vector(loneElement)) {
  def asEvery: Every[T] = this
  def ++[U >: T](other: Every[U]): Many[U] = Many(loneElement, other.toVector.head, other.toVector.tail: _*)
  def ++[U >: T](other: GenTraversableOnce[U]): Every[U] =
    if (other.isEmpty) this else Many(loneElement, other.toVector.head, other.toVector.tail: _*)
  def :+[U >: T](element: U): Many[U] = Many(loneElement, element)
  def stringPrefix: String = "One"
  override def toString: String = "One(" + loneElement + ")"
}

/**
 * An <code>Every</code> that contains two or more elements.
 *
 * <p>
 * For more information and examples, see the main documentation for superclass <a href="Every.html"><code>Every</code></a>.
 * </p>
 *
 * @tparam T the type of the element contained in this <code>Many</code>
 * @param firstElement the first element (with index 0) contained in this <code>Many</code>
 * @param secondElement the second element (with index 1) contained in this <code>Many</code>
 * @param otherElement a varargs of zero or more other elements (with index 2, 3, ...) contained in this <code>Many</code>
 */
final case class Many[+T](firstElement: T, secondElement: T, otherElements: T*) extends Every[T](firstElement +: secondElement +: Vector(otherElements: _*)) {
  def asEvery: Every[T] = this
  def ++[U >: T](other: Every[U]): Many[U] = Many(firstElement, secondElement, (otherElements.toVector ++ other.toVector): _*)
  def ++[U >: T](other: GenTraversableOnce[U]): Every[U] =
    if (other.isEmpty) this else Many(firstElement, secondElement, otherElements ++ other.toVector: _*)
  def :+[U >: T](element: U): Many[U] = Many(firstElement, secondElement, (otherElements :+ element): _*)
  def stringPrefix: String = "Many"
  override def toString: String = "Many(" + toVector.mkString(", ") + ")"
}

