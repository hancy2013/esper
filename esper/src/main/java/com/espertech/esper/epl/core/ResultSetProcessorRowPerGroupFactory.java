/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.core;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.service.AggregationService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.spec.OutputLimitLimitType;
import com.espertech.esper.epl.spec.OutputLimitSpec;

/**
 * Result set processor prototype for the fully-grouped case:
 * there is a group-by and all non-aggregation event properties in the select clause are listed in the group by,
 * and there are aggregation functions.
 */
public class ResultSetProcessorRowPerGroupFactory implements ResultSetProcessorFactory
{
    private final SelectExprProcessor selectExprProcessor;
    private final ExprNode[] groupKeyNodeExpressions;
    private final ExprEvaluator groupKeyNode;
    private final ExprEvaluator[] groupKeyNodes;
    private final ExprEvaluator optionalHavingNode;
    private final boolean isSorting;
    private final boolean isSelectRStream;
    private final boolean isUnidirectional;
    private final OutputLimitSpec outputLimitSpec;
    private final boolean noDataWindowSingleSnapshot;
    private final boolean isHistoricalOnly;

    /**
     * Ctor.
     * @param selectExprProcessor - for processing the select expression and generting the final output rows
     * @param groupKeyNodes - list of group-by expression nodes needed for building the group-by keys
     * @param optionalHavingNode - expression node representing validated HAVING clause, or null if none given.
     * Aggregation functions in the having node must have been pointed to the AggregationService for evaluation.
     * @param isSelectRStream - true if remove stream events should be generated
     * @param isUnidirectional - true if unidirectional join
     */
    public ResultSetProcessorRowPerGroupFactory(SelectExprProcessor selectExprProcessor,
                                                ExprNode[] groupKeyNodeExpressions,
                                                ExprEvaluator[] groupKeyNodes,
                                                ExprEvaluator optionalHavingNode,
                                                boolean isSelectRStream,
                                                boolean isUnidirectional,
                                                OutputLimitSpec outputLimitSpec,
                                                boolean isSorting,
                                                boolean noDataWindowSingleStream,
                                                boolean isHistoricalOnly,
                                                boolean iterateUnbounded)
    {
        this.groupKeyNodeExpressions = groupKeyNodeExpressions;
        this.selectExprProcessor = selectExprProcessor;
        this.groupKeyNodes = groupKeyNodes;
        if (groupKeyNodes.length == 1) {
            this.groupKeyNode = groupKeyNodes[0];
        }
        else {
            this.groupKeyNode = null;
        }
        this.optionalHavingNode = optionalHavingNode;
        this.isSorting = isSorting;
        this.isSelectRStream = isSelectRStream;
        this.isUnidirectional = isUnidirectional;
        this.outputLimitSpec = outputLimitSpec;
        this.noDataWindowSingleSnapshot = iterateUnbounded || (outputLimitSpec != null && outputLimitSpec.getDisplayLimit() == OutputLimitLimitType.SNAPSHOT && noDataWindowSingleStream);
        this.isHistoricalOnly = isHistoricalOnly;
    }

    public ResultSetProcessor instantiate(OrderByProcessor orderByProcessor, AggregationService aggregationService, AgentInstanceContext agentInstanceContext) {
        if (noDataWindowSingleSnapshot && !isHistoricalOnly) {
            return new ResultSetProcessorRowPerGroupUnbound(this, selectExprProcessor, orderByProcessor, aggregationService, agentInstanceContext);
        }
        return new ResultSetProcessorRowPerGroup(this, selectExprProcessor, orderByProcessor, aggregationService, agentInstanceContext);
    }

    public EventType getResultEventType()
    {
        return selectExprProcessor.getResultEventType();
    }

    public boolean hasAggregation() {
        return true;
    }

    public ExprEvaluator[] getGroupKeyNodes() {
        return groupKeyNodes;
    }

    public ExprEvaluator getGroupKeyNode() {
        return groupKeyNode;
    }

    public ExprEvaluator getOptionalHavingNode() {
        return optionalHavingNode;
    }

    public boolean isSorting() {
        return isSorting;
    }

    public boolean isSelectRStream() {
        return isSelectRStream;
    }

    public boolean isUnidirectional() {
        return isUnidirectional;
    }

    public OutputLimitSpec getOutputLimitSpec() {
        return outputLimitSpec;
    }

    public ExprNode[] getGroupKeyNodeExpressions() {
        return groupKeyNodeExpressions;
    }

    public boolean isHistoricalOnly() {
        return isHistoricalOnly;
    }

    public ResultSetProcessorType getResultSetProcessorType() {
        return ResultSetProcessorType.FULLYAGGREGATED_GROUPED;
    }

    public boolean isOutputLast() {
        return outputLimitSpec != null && outputLimitSpec.getDisplayLimit() == OutputLimitLimitType.LAST;
    }

    public boolean isOutputAll() {
        return outputLimitSpec != null && outputLimitSpec.getDisplayLimit() == OutputLimitLimitType.ALL;
    }
}
