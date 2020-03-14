package com.github.j5ik2o.akka.persistence.kafka.utils

object EitherSeq {

  def sequence[A](seq: Seq[Either[Throwable, A]]): Either[Throwable, Seq[A]] = {

    @scala.annotation.tailrec
    def recurse(remaining: Seq[Either[Throwable, A]], processed: Seq[A]): Either[Throwable, Seq[A]] = remaining match {
      case Seq()               => Right(processed)
      case Right(head) +: tail => recurse(remaining = tail, processed :+ head)
      case Left(t) +: _        => Left(t)
    }
    recurse(seq, Vector.empty)
  }

}
