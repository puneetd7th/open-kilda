package org.bitbucket.openkilda.wfm.topology.flow.bolts;

import static org.bitbucket.openkilda.wfm.topology.AbstractTopology.MESSAGE_FIELD;
import static org.bitbucket.openkilda.wfm.topology.AbstractTopology.fieldMessage;
import static org.bitbucket.openkilda.wfm.topology.flow.FlowTopology.FLOW_ID_FIELD;
import static org.bitbucket.openkilda.wfm.topology.flow.FlowTopology.STATUS_FIELD;
import static org.bitbucket.openkilda.wfm.topology.flow.FlowTopology.fieldsMessageErrorType;
import static org.bitbucket.openkilda.wfm.topology.flow.FlowTopology.fieldsMessageFlowId;

import org.bitbucket.openkilda.messaging.Destination;
import org.bitbucket.openkilda.messaging.Message;
import org.bitbucket.openkilda.messaging.Utils;
import org.bitbucket.openkilda.messaging.error.ErrorData;
import org.bitbucket.openkilda.messaging.error.ErrorMessage;
import org.bitbucket.openkilda.messaging.error.ErrorType;
import org.bitbucket.openkilda.messaging.info.InfoMessage;
import org.bitbucket.openkilda.messaging.info.flow.FlowStatusResponse;
import org.bitbucket.openkilda.messaging.payload.flow.FlowIdStatusPayload;
import org.bitbucket.openkilda.messaging.payload.flow.FlowState;
import org.bitbucket.openkilda.wfm.topology.flow.ComponentType;
import org.bitbucket.openkilda.wfm.topology.flow.StreamType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.storm.state.InMemoryKeyValueState;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseStatefulBolt;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

import java.util.Map;

/**
 * Status Bolt. Tracks flows status.
 */
public class StatusBolt extends BaseStatefulBolt<InMemoryKeyValueState<String, FlowState>> {
    /**
     * The logger.
     */
    private static final Logger logger = LogManager.getLogger(StatusBolt.class);

    /**
     * Flows state.
     */
    private InMemoryKeyValueState<String, FlowState> flowStates;

    /**
     * Output collector.
     */
    private OutputCollector outputCollector;

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(Tuple tuple) {
        logger.trace("States before: {}", flowStates);
        logger.debug("Ingoing tuple: {}", tuple);

        ComponentType componentId = ComponentType.valueOf(tuple.getSourceComponent());
        StreamType streamId = StreamType.valueOf(tuple.getSourceStreamId());
        String flowId = (String) tuple.getValueByField(FLOW_ID_FIELD);
        FlowState flowStatus = null;
        Values values;
        Message message;
        ErrorMessage errorMessage;
        String logMessage;

        try {
            switch (componentId) {

                case SPLITTER_BOLT:
                    message = (Message) tuple.getValueByField(MESSAGE_FIELD);

                    switch (streamId) {

                        case CREATE:
                            flowStatus = flowStates.get(flowId);

                            if (flowStatus == null) {
                                logger.debug("Flow creation message: {}={}, {}={}, component={}, stream={}",
                                        Utils.CORRELATION_ID, message.getCorrelationId(),
                                        Utils.FLOW_ID, flowId, componentId, streamId);

                                flowStates.put(flowId, FlowState.ALLOCATED);

                                message.setDestination(Destination.TOPOLOGY_ENGINE);
                                values = new Values(message, flowId);
                                outputCollector.emit(StreamType.CREATE.toString(), tuple, values);

                            } else {
                                logMessage = String.format("Flow already exists: %s=%s", Utils.FLOW_ID, flowId);
                                logger.error("{}, {}={}, component={}, stream={}", logMessage,
                                        Utils.CORRELATION_ID, message.getCorrelationId(), componentId, streamId);

                                errorMessage = getErrorMessage(message.getCorrelationId(),
                                        ErrorType.ALREADY_EXISTS, logMessage, componentId.toString().toLowerCase());

                                values = new Values(errorMessage, ErrorType.ALREADY_EXISTS);
                                outputCollector.emit(StreamType.ERROR.toString(), tuple, values);
                            }
                            break;

                        case UPDATE:
                            flowStatus = flowStates.get(flowId);

                            if (flowStatus != null) {
                                logger.debug("Flow update message: {}={}, {}={}, component={}, stream={}",
                                        Utils.CORRELATION_ID, message.getCorrelationId(),
                                        Utils.FLOW_ID, flowId, componentId, streamId);

                                flowStates.put(flowId, FlowState.ALLOCATED);

                                message.setDestination(Destination.TOPOLOGY_ENGINE);
                                values = new Values(message, flowId);
                                outputCollector.emit(StreamType.UPDATE.toString(), tuple, values);

                            } else {
                                logMessage = String.format("Flow not found: %s=%s", Utils.FLOW_ID, flowId);
                                logger.error("{}, {}={}, component={}, stream={}", logMessage,
                                        Utils.CORRELATION_ID, message.getCorrelationId(), componentId, streamId);

                                errorMessage = getErrorMessage(message.getCorrelationId(),
                                        ErrorType.NOT_FOUND, logMessage, componentId.toString().toLowerCase());

                                values = new Values(errorMessage, ErrorType.NOT_FOUND);
                                outputCollector.emit(StreamType.ERROR.toString(), tuple, values);
                            }
                            break;

                        case DELETE:
                            flowStatus = flowStates.get(flowId);

                            if (flowStatus != null) {
                                logger.debug("Flow delete message: {}={}, {}={}, component={}, stream={}",
                                        Utils.CORRELATION_ID, message.getCorrelationId(),
                                        Utils.FLOW_ID, flowId, componentId, streamId);

                                flowStates.delete(flowId);

                                message.setDestination(Destination.TOPOLOGY_ENGINE);
                                values = new Values(message, flowId);
                                outputCollector.emit(StreamType.DELETE.toString(), tuple, values);

                            } else {
                                logMessage = String.format("Flow not found: %s=%s", Utils.FLOW_ID, flowId);
                                logger.error("{}, {}={}, component={}, stream={}", logMessage,
                                        Utils.CORRELATION_ID, message.getCorrelationId(), componentId, streamId);

                                errorMessage = getErrorMessage(message.getCorrelationId(),
                                        ErrorType.NOT_FOUND, logMessage, componentId.toString().toLowerCase());

                                values = new Values(errorMessage, ErrorType.NOT_FOUND);
                                outputCollector.emit(StreamType.ERROR.toString(), tuple, values);
                            }
                            break;

                        case RESTORE:
                            flowStatus = flowStates.get(flowId);

                            if (flowStatus == null) {
                                logger.debug("Flow restore message: {}={}, {}={}, component={}, stream={}",
                                        Utils.CORRELATION_ID, message.getCorrelationId(),
                                        Utils.FLOW_ID, flowId, componentId, streamId);

                                flowStates.put(flowId, FlowState.ALLOCATED);

                                message.setDestination(Destination.TOPOLOGY_ENGINE);
                                values = new Values(message, flowId);
                                outputCollector.emit(StreamType.CREATE.toString(), tuple, values);

                            } else {
                                logMessage = String.format("Flow already exists: %s=%s", Utils.FLOW_ID, flowId);
                                logger.error("{}, {}={}, component={}, stream={}", logMessage,
                                        Utils.CORRELATION_ID, message.getCorrelationId(), componentId, streamId);

                                errorMessage = getErrorMessage(message.getCorrelationId(),
                                        ErrorType.ALREADY_EXISTS, logMessage, componentId.toString().toLowerCase());

                                values = new Values(errorMessage, ErrorType.ALREADY_EXISTS);
                                outputCollector.emit(StreamType.ERROR.toString(), tuple, values);
                            }
                            break;

                        case REROUTE:
                            flowStatus = flowStates.get(flowId);

                            if (flowStatus != null) {
                                logger.debug("Flow reroute message: {}={}, {}={}, component={}, stream={}",
                                        Utils.CORRELATION_ID, message.getCorrelationId(),
                                        Utils.FLOW_ID, flowId, componentId, streamId);

                                flowStates.put(flowId, FlowState.ALLOCATED);

                                message.setDestination(Destination.TOPOLOGY_ENGINE);
                                values = new Values(message, flowId);
                                outputCollector.emit(StreamType.UPDATE.toString(), tuple, values);

                            } else {
                                logMessage = String.format("Flow not found: %s=%s", Utils.FLOW_ID, flowId);
                                logger.error("{}, {}={}, component={}, stream={}", logMessage,
                                        Utils.CORRELATION_ID, message.getCorrelationId(), componentId, streamId);

                                errorMessage = getErrorMessage(message.getCorrelationId(),
                                        ErrorType.NOT_FOUND, logMessage, componentId.toString().toLowerCase());

                                values = new Values(errorMessage, ErrorType.NOT_FOUND);
                                outputCollector.emit(StreamType.ERROR.toString(), tuple, values);
                            }
                            break;

                        case READ:
                            if (flowId != null) {
                                flowStatus = flowStates.get(flowId);
                            }

                            if (flowStatus != null || flowId == null) {
                                logger.debug("Flow get message: {}={}, {}={}, component={}, stream={}",
                                        Utils.CORRELATION_ID, message.getCorrelationId(),
                                        Utils.FLOW_ID, flowId, componentId, streamId);

                                values = new Values(message, flowId);
                                outputCollector.emit(StreamType.READ.toString(), tuple, values);

                            } else {
                                logMessage = String.format("Flow not found: %s=%s", Utils.FLOW_ID, flowId);
                                logger.error("{}, {}={}, component={}, stream={}", logMessage,
                                        Utils.CORRELATION_ID, message.getCorrelationId(), componentId, streamId);

                                errorMessage = getErrorMessage(message.getCorrelationId(),
                                        ErrorType.NOT_FOUND, logMessage, componentId.toString().toLowerCase());

                                values = new Values(errorMessage, ErrorType.NOT_FOUND);
                                outputCollector.emit(StreamType.ERROR.toString(), tuple, values);
                            }
                            break;

                        case PATH:
                            flowStatus = flowStates.get(flowId);

                            if (flowStatus != null) {
                                logger.debug("Flow path message: {}={}, {}={}, component={}, stream={}",
                                        Utils.CORRELATION_ID, message.getCorrelationId(),
                                        Utils.FLOW_ID, flowId, componentId, streamId);

                                values = new Values(message, flowId);
                                outputCollector.emit(StreamType.PATH.toString(), tuple, values);

                            } else {
                                logMessage = String.format("Flow not found: %s=%s", Utils.FLOW_ID, flowId);
                                logger.error("{}, {}={}, component={}, stream={}", logMessage,
                                        Utils.CORRELATION_ID, message.getCorrelationId(), componentId, streamId);

                                errorMessage = getErrorMessage(message.getCorrelationId(),
                                        ErrorType.NOT_FOUND, logMessage, componentId.toString().toLowerCase());

                                values = new Values(errorMessage, ErrorType.NOT_FOUND);
                                outputCollector.emit(StreamType.ERROR.toString(), tuple, values);
                            }
                            break;

                        case STATUS:
                            flowStatus = flowStates.get(flowId);

                            if (flowStatus != null) {
                                logger.debug("Flow status message: {}={}, {}={}, component={}, stream={}",
                                        Utils.CORRELATION_ID, message.getCorrelationId(),
                                        Utils.FLOW_ID, flowId, componentId, streamId);

                                InfoMessage responseMessage = new InfoMessage(
                                        new FlowStatusResponse(new FlowIdStatusPayload(flowId, flowStatus)),
                                        message.getTimestamp(), message.getCorrelationId(), Destination.NORTHBOUND);

                                values = new Values(responseMessage);
                                outputCollector.emit(StreamType.RESPONSE.toString(), tuple, values);

                            } else {
                                logMessage = String.format("Flow not found: %s=%s", Utils.FLOW_ID, flowId);
                                logger.error("{}, {}={}, component={}, stream={}", logMessage,
                                        Utils.CORRELATION_ID, message.getCorrelationId(), componentId, streamId);

                                errorMessage = getErrorMessage(message.getCorrelationId(),
                                        ErrorType.NOT_FOUND, logMessage, componentId.toString().toLowerCase());

                                values = new Values(errorMessage, ErrorType.NOT_FOUND);
                                outputCollector.emit(StreamType.ERROR.toString(), tuple, values);
                            }
                            break;

                        default:
                            logger.warn("Skip message from unknown stream: {}={}, {}={}, component={}, stream={}",
                                    Utils.CORRELATION_ID, message.getCorrelationId(),
                                    Utils.FLOW_ID, flowId, componentId, streamId);
                            break;
                    }
                    break;

                case TOPOLOGY_ENGINE_BOLT:
                    errorMessage = (ErrorMessage) tuple.getValueByField(MESSAGE_FIELD);
                    ErrorData errorData = errorMessage.getData();

                    switch (errorData.getErrorType()) {

                        case CREATION_FAILURE:
                            logger.info("Flow {} creation failure: component={}, stream={}",
                                    flowId, componentId, streamId);
                            flowStates.delete(flowId);
                            break;

                        case UPDATE_FAILURE:
                            logger.info("Flow {} update failure: component={}, stream={}",
                                    flowId, componentId, streamId);
                            flowStates.put(flowId, FlowState.DOWN);
                            break;

                        case DELETION_FAILURE:
                            logger.info("Flow {} deletion failure: component={}, stream={}",
                                    flowId, componentId, streamId);
                            break;

                        case INTERNAL_ERROR:
                        default:
                            logger.warn("Flow {} undefined failure: component={}, stream={}",
                                    flowId, componentId, streamId);
                            break;
                    }

                    errorMessage.setDestination(Destination.NORTHBOUND);
                    errorData.setErrorDescription(componentId.toString().toLowerCase());
                    values = new Values(errorMessage);
                    outputCollector.emit(StreamType.RESPONSE.toString(), tuple, values);

                    break;

                case SPEAKER_BOLT:

                case TRANSACTION_BOLT:
                    FlowState newStatus = (FlowState) tuple.getValueByField(STATUS_FIELD);
                    flowStatus = flowStates.get(flowId);

                    if (flowStatus != null) {
                        logger.debug("Flow {} status {}: component={}, stream={}",
                                flowId, newStatus, componentId, streamId);
                        flowStates.put(flowId, newStatus);
                    } else {
                        logger.error("Flow {} not found: component={}, stream={}, status={}",
                                flowId, componentId, streamId, newStatus);
                    }
                    break;

                default:
                    logger.error("Skip undefined message: {}={}, component={}, stream={}",
                            Utils.FLOW_ID, flowId, componentId, streamId);
                    break;
            }
        } finally {
            logger.debug("Flow message ack: {}={}, component={}, stream={}",
                    Utils.FLOW_ID, flowId, componentId, streamId);

            outputCollector.ack(tuple);

            logger.trace("States after: {}", flowStates);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initState(InMemoryKeyValueState<String, FlowState> state) {
        flowStates = state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declareStream(StreamType.CREATE.toString(), fieldsMessageFlowId);
        outputFieldsDeclarer.declareStream(StreamType.UPDATE.toString(), fieldsMessageFlowId);
        outputFieldsDeclarer.declareStream(StreamType.DELETE.toString(), fieldsMessageFlowId);
        outputFieldsDeclarer.declareStream(StreamType.READ.toString(), fieldsMessageFlowId);
        outputFieldsDeclarer.declareStream(StreamType.PATH.toString(), fieldsMessageFlowId);
        outputFieldsDeclarer.declareStream(StreamType.RESPONSE.toString(), fieldMessage);
        outputFieldsDeclarer.declareStream(StreamType.ERROR.toString(), fieldsMessageErrorType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        this.outputCollector = outputCollector;
    }

    private ErrorMessage getErrorMessage(final String correlationId, final ErrorType errorType,
                                         final String errorMessage, final String errorDescription) {
        return new ErrorMessage(new ErrorData(errorType, errorMessage, errorDescription),
                System.currentTimeMillis(), correlationId, Destination.NORTHBOUND);
    }
}
