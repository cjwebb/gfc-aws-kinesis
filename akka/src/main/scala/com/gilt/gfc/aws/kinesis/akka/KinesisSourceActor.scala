package com.gilt.gfc.aws.kinesis.akka

import com.amazonaws.services.kinesis.model.Record
import com.gilt.gfc.aws.kinesis.client.{KCLConfiguration, KCLWorkerRunner, KinesisRecordReader}

class KinesisStreamConsumer[T](
  streamConfig: KinesisStreamConsumerConfig[T],
  val handler: KinesisStreamHandler[T]
) {
  val kclConfig = KCLConfiguration(
    streamConfig.applicationName,
    streamConfig.streamName,
    streamConfig.kinesisCredentialsProvider,
    streamConfig.dynamoCredentialsProvider,
    streamConfig.cloudWatchCredentialsProvider
  )

  def createWorker = KCLWorkerRunner(
    kclConfig,
    metricsFactory = Some(streamConfig.metricsFactory),
    checkpointInterval = streamConfig.checkPointInterval,
    initialize = handler.onInit,
    shutdown = handler.onShutdown,
    initialRetryDelay = streamConfig.retryConfig.initialDelay,
    maxRetryDelay = streamConfig.retryConfig.retryDelay,
    numRetries = streamConfig.retryConfig.maxRetries
  )

  implicit val messageDeserializer = new KinesisRecordReader[T] {
    override def apply(r: Record): T = streamConfig.recordDeserializer(r.getData.array())
  }

  def run() = {
    val worker = createWorker
    worker.runSingleRecordProcessor(handler.onRecord)
  }
}

