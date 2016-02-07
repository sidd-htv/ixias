/*
 * This file is part of the IxiaS services.
 *
 * For the full copyright and license information,
 * please view the LICENSE file that was distributed with this source code.
 */

package net.ixias
package core.port.adapter.persistence.lifted

import scala.annotation.tailrec
import scala.language.implicitConversions
import core.domain.model.Entity
import core.port.adapter.persistence.backend.DataConverter

trait DataConvertOps {
  // Transform a record to a domain model object.
  implicit def toModel[E <: Entity[_], R](value: E)
    (implicit f: DataConverter[E, R]): R = f.convert(value)

  implicit def toModel[E <: Entity[_], R](value: Option[E])
    (implicit f: DataConverter[E, R]): Option[R] = value.map(f.convert)

  implicit def toModels[E <: Entity[_], R](values: Seq[E])
    (implicit f: DataConverter[E, R]): Seq[R] = toModels(values, Seq())

  @tailrec final def toModels[E <: Entity[_], R](values: Seq[E], results: Seq[R])
    (implicit f: DataConverter[E, R]): Seq[R] =
    values match {
      case head :: tail => toModels(tail, results :+ f.convert(head))
      case head +: tail => toModels(tail, results :+ f.convert(head))
      case _ => results
    }

  // Transform a domain model object to a record.
  implicit def toRecord[R, E <: Entity[_]](value: R)
    (implicit f: DataConverter[E, R]): E = f.convert(value)

  implicit def toRecord[R, E <: Entity[_]](value: Option[R])
    (implicit f: DataConverter[E, R]): Option[E] = value.map(f.convert)

  implicit def toRecords[R, E <: Entity[_]](values: Seq[R])
    (implicit f: DataConverter[E, R]): Seq[E] = toRecords(values, Seq())

  @tailrec final def toRecords[R, E <: Entity[_]](values: Seq[R], results: Seq[E])
    (implicit f: DataConverter[E, R]): Seq[E] =
    values match {
      case head :: tail => toRecords(tail, results :+ f.convert(head))
      case head +: tail => toRecords(tail, results :+ f.convert(head))
      case _ => results
    }
}
