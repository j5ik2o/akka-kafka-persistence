package com.github.j5ik2o.akka.persistence.kafka.journal

import akka.kafka.ConsumerSettings
import com.github.j5ik2o.akka.persistence.kafka.resolver.{ KafkaPartitionResolver, KafkaTopicResolver }
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.TopicPartition

import scala.concurrent.{ ExecutionContext, Future }
import scala.jdk.CollectionConverters._

class JournalSequence(
    consumerSettings: ConsumerSettings[String, Array[Byte]],
    topicPrefix: String,
    journalTopicResolver: KafkaTopicResolver,
    journalPartitionResolver: KafkaPartitionResolver
) {
  private val consumer: Consumer[String, Array[Byte]] = consumerSettings.createKafkaConsumer()

  def close(): Unit = synchronized {
    consumer.close()
  }

  def readHighestSequenceNrAsync(persistenceId: PersistenceId, fromSequenceNr: Option[Long] = None)(
      implicit ec: ExecutionContext
  ): Future[Long] =
    Future { readHighestSequenceNr(persistenceId, fromSequenceNr) }

  def readHighestSequenceNr(
      persistenceId: PersistenceId,
      fromSequenceNr: Option[Long] = None
  ): Long = synchronized {
    val topic         = topicPrefix + journalTopicResolver.resolve(persistenceId).asString
    val partitionSize = consumer.partitionsFor(topic).asScala.size
    val partitonId    = journalPartitionResolver.resolve(partitionSize, persistenceId).value

    val tp = new TopicPartition(topic, partitonId)
    consumer.assign(List(tp).asJava)
    fromSequenceNr.foreach(consumer.seek(tp, _))
    val result =
      consumer.endOffsets(List(tp).asJava).get(tp)
    Math.max(result, 0)
  }

}
