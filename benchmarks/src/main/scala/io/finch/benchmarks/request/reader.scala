package io.finch.benchmarks.request

import com.twitter.finagle.httpx.Request
import com.twitter.util.{Await, Try}
import io.finch.request._
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._

case class Foo(s: String, d: Double, i: Int, l: Long, b: Boolean)

/**
 * Compares the performance of different ways of creating request readers on
 * valid input.
 *
 * The following command will run the request reader benchmarks with reasonable
 * settings:
 *
 * > sbt "benchmarks/run -i 10 -wi 10 -f 2 -t 1 io.finch.benchmarks.request.*"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class SuccessfulRequestReaderBenchmark extends FooReadersAndRequests {
  @Benchmark
  def monadicReader: Foo = Await.result(monadicFooReader(goodFooRequest))

  @Benchmark
  def tildeReader: Foo = Await.result(tildeFooReader(goodFooRequest))

  @Benchmark
  def hlistGenericReader: Foo = Await.result(hlistGenericFooReader(goodFooRequest))

  @Benchmark
  def hlistApplyReader: Foo = Await.result(hlistApplyFooReader(goodFooRequest))

  @Benchmark
  def derivedReader: Foo = Await.result(derivedFooReader(goodFooRequest))
}

/**
 * Compares the performance of different ways of creating request readers on
 * invalid input.
 *
 * Note that the monadic reader shouldn't be compared directly to the other
 * readers for invalid inputs, since it fails on the first error.
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class FailingRequestReaderBenchmark extends FooReadersAndRequests {
  @Benchmark
  def monadicReader: Try[Foo] = Await.result(monadicFooReader(badFooRequest).liftToTry)

  @Benchmark
  def tildeReader: Try[Foo] = Await.result(tildeFooReader(badFooRequest).liftToTry)

  @Benchmark
  def hlistGenericReader: Try[Foo] = Await.result(hlistGenericFooReader(badFooRequest).liftToTry)

  @Benchmark
  def hlistApplyReader: Try[Foo] = Await.result(hlistApplyFooReader(badFooRequest).liftToTry)

  @Benchmark
  def derivedReader: Try[Foo] = Await.result(derivedFooReader(badFooRequest).liftToTry)
}

/**
 * Provides request readers and example requests.
 */
class FooReadersAndRequests {
  val monadicFooReader: RequestReader[Foo] = for {
    s <- param("s")
    d <- param("d").as[Double]
    i <- param("i").as[Int]
    l <- param("l").as[Long]
    b <- param("b").as[Boolean]
  } yield Foo(s, d, i, l, b)

  val tildeFooReader: RequestReader[Foo] =
    param("s") ~
    param("d").as[Double] ~
    param("i").as[Int] ~
    param("l").as[Long] ~
    param("b").as[Boolean] ~>
    Foo

  val hlistGenericFooReader: RequestReader[Foo] = (
    param("s") ::
    param("d").as[Double] ::
    param("i").as[Int] ::
    param("l").as[Long] ::
    param("b").as[Boolean]
  ).as[Foo]

  val hlistApplyFooReader: RequestReader[Foo] = (
    param("s") ::
    param("d").as[Double] ::
    param("i").as[Int] ::
    param("l").as[Long] ::
    param("b").as[Boolean]
  ) ~> Foo.apply _

  val derivedFooReader: RequestReader[Foo] = RequestReader.derive[Foo].fromParams

  val goodFooRequest: Request = Request(
    "s" -> "Man hands on misery to man. It deepens like a coastal shelf.",
    "d" -> "0.234567",
    "i" -> "123456",
    "l" -> "1234567890",
    "b" -> "true"
  )

  val badFooRequest: Request = Request(
    "s" -> "Man hands on misery to man. It deepens like a coastal shelf.",
    "d" -> "0.23h4567",
    "i" -> "123456",
    "l" -> "1234567890x",
    "b" -> "true"
  )

  val goodFooResult: Foo = Foo(
    "Man hands on misery to man. It deepens like a coastal shelf.",
    0.234567,
    123456,
    1234567890L,
    true
  )
}
