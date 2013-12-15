/**
 * Copyright 2013 DuraSpace, Inc.
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

package org.fcrepo.transform.sparql;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryVisitor;
import com.hp.hpl.jena.query.SortCondition;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.core.Prologue;
import com.hp.hpl.jena.sparql.core.TriplePath;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprAggregator;
import com.hp.hpl.jena.sparql.expr.ExprFunction0;
import com.hp.hpl.jena.sparql.expr.ExprFunction1;
import com.hp.hpl.jena.sparql.expr.ExprFunction2;
import com.hp.hpl.jena.sparql.expr.ExprFunction3;
import com.hp.hpl.jena.sparql.expr.ExprFunctionN;
import com.hp.hpl.jena.sparql.expr.ExprFunctionOp;
import com.hp.hpl.jena.sparql.expr.ExprVar;
import com.hp.hpl.jena.sparql.expr.ExprVisitor;
import com.hp.hpl.jena.sparql.expr.FunctionLabel;
import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.syntax.Element;
import com.hp.hpl.jena.sparql.syntax.ElementAssign;
import com.hp.hpl.jena.sparql.syntax.ElementBind;
import com.hp.hpl.jena.sparql.syntax.ElementData;
import com.hp.hpl.jena.sparql.syntax.ElementDataset;
import com.hp.hpl.jena.sparql.syntax.ElementExists;
import com.hp.hpl.jena.sparql.syntax.ElementFilter;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.ElementMinus;
import com.hp.hpl.jena.sparql.syntax.ElementNamedGraph;
import com.hp.hpl.jena.sparql.syntax.ElementNotExists;
import com.hp.hpl.jena.sparql.syntax.ElementOptional;
import com.hp.hpl.jena.sparql.syntax.ElementPathBlock;
import com.hp.hpl.jena.sparql.syntax.ElementService;
import com.hp.hpl.jena.sparql.syntax.ElementSubQuery;
import com.hp.hpl.jena.sparql.syntax.ElementTriplesBlock;
import com.hp.hpl.jena.sparql.syntax.ElementUnion;
import com.hp.hpl.jena.sparql.syntax.ElementVisitor;
import org.apache.commons.lang.NotImplementedException;
import org.fcrepo.kernel.rdf.JcrRdfTools;
import org.fcrepo.kernel.utils.NodePropertiesTools;
import org.fcrepo.transform.exception.JQLParsingException;
import org.modeshape.common.collection.Collections;
import org.modeshape.jcr.api.query.qom.Limit;
import org.modeshape.jcr.api.query.qom.SelectQuery;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.QueryManager;
import javax.jcr.query.qom.Column;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.JoinCondition;
import javax.jcr.query.qom.Literal;
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.PropertyValue;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.Source;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Sets.difference;
import static com.google.common.primitives.Ints.checkedCast;
import static com.hp.hpl.jena.query.Query.ORDER_DESCENDING;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static java.lang.Integer.MAX_VALUE;
import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.URI;
import static javax.jcr.PropertyType.WEAKREFERENCE;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_JOIN_TYPE_INNER;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_JOIN_TYPE_LEFT_OUTER;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_LIKE;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_NOT_EQUAL_TO;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_RESOURCE;
import static org.modeshape.jcr.api.JcrConstants.JCR_PATH;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implements the Jena QueryVisitor pattern to translate a SPARQL query into
 * a JCR query
 *
 * @author cabeer
 */
public class JQLQueryVisitor implements QueryVisitor, ElementVisitor, ExprVisitor {

    private static final Logger LOGGER = getLogger(JQLQueryVisitor.class);

    private QueryObjectModelFactory queryFactory;
    private Source source;
    private ImmutableSet.Builder<Column> columns;
    private ImmutableList.Builder<Ordering>  orderings;
    private Constraint constraint;
    private boolean hasLimit = false;
    private long offset;
    private long limit;
    private Session session;
    private JcrRdfTools jcrTools;
    private Set<String> resultsVars;
    private Map<String, Column> variables;
    private boolean distinct;
    private boolean inOptional;
    private Map<String, Source> joins;
    private Map<String, String> joinTypes;
    private Map<String, JoinCondition> joinConditions;

    private NodePropertiesTools propertiesTools = new NodePropertiesTools();

    /**
     * Create a new query
     * @param session
     * @param jcrTools
     * @param queryManager
     * @throws RepositoryException
     */
    public JQLQueryVisitor(final Session session,
                           final JcrRdfTools jcrTools,
                           final QueryManager queryManager) throws RepositoryException {
        this.session = session;
        this.jcrTools = jcrTools;
        this.queryFactory = queryManager.getQOMFactory();
        this.constraint = null;
        this.variables = new HashMap<String, Column>();
        this.joins = new HashMap<String, Source>();
        this.joinTypes = new HashMap<>();
        this.joinConditions = new HashMap<String, JoinCondition>();
    }

    /**
     * Create a subquery, using the same variables, joins, etc, but without existing constraints
     *
     * @param jqlQueryVisitor
     */
    public JQLQueryVisitor(final JQLQueryVisitor jqlQueryVisitor) {
        this.session = jqlQueryVisitor.session;
        this.jcrTools = jqlQueryVisitor.jcrTools;
        this.queryFactory = jqlQueryVisitor.queryFactory;
        this.constraint = null;
        this.variables = jqlQueryVisitor.variables;
        this.joins = jqlQueryVisitor.joins;
        this.joinConditions = jqlQueryVisitor.joinConditions;
    }

    /**
     * Get the raw JCR query
     * @return
     * @throws RepositoryException
     */
    public QueryObjectModel getQuery() throws RepositoryException {
        final org.modeshape.jcr.api.query.qom.QueryObjectModelFactory modeQueryFactory =
            (org.modeshape.jcr.api.query.qom.QueryObjectModelFactory)queryFactory;
        final int actualLimit;

        if (this.hasLimit) {
            actualLimit = checkedCast(this.limit);
        } else {
            actualLimit = MAX_VALUE;
        }

        final Limit selectLimit = modeQueryFactory.limit(actualLimit, checkedCast(this.offset));
        final SelectQuery query = modeQueryFactory.select(getSource(),
                                                             getConstraint(),
                                                             getOrderings(),
                                                             getColumns(),
                                                             selectLimit,
                                                             distinct);


        return modeQueryFactory.createQuery(query);
    }

    /**
     * Get the JCR query source information
     * @return
     */
    private Source getSource() {
        final Sets.SetView<String> difference = difference(joins.keySet(), joinConditions.keySet());

        final Source parentSource;

        final Iterator<String> unmatchedJoins = difference.iterator();

        if (unmatchedJoins.hasNext()) {
            parentSource = joins.get(unmatchedJoins.next());
            this.source = parentSource;
        } else {
            throw new JQLParsingException("No primary source column found in query");
        }

        try {
            for (final Map.Entry<String, Source> entry : joins.entrySet()) {

                if (entry.getValue() != parentSource) {
                    final String joinType;

                    if (joinTypes.containsKey(entry.getKey())) {
                        joinType = JCR_JOIN_TYPE_INNER;
                    } else {
                        joinType = JCR_JOIN_TYPE_LEFT_OUTER;
                    }

                    this.source =
                        queryFactory.join(this.source, entry.getValue(),
                                joinType, joinConditions.get(entry.getKey()));
                }
            }

        } catch (final RepositoryException e) {
            LOGGER.info(e.getMessage());
        }
        return this.source;
    }

    /**
     * Get the columns for the JCR query
     * @return
     */
    private Column[] getColumns() {
        final ImmutableSet<Column> build = this.columns.build();
        return build.toArray(new Column[build.size()]);
    }

    /**
     * Get the ordering of the JCR query
     * @return
     */
    private Ordering[] getOrderings() {
        final ImmutableList<Ordering> build = this.orderings.build();
        return build.toArray(new Ordering[build.size()]);
    }

    /**
     * Get the constraints imposed on the JCR query
     * @return
     */
    private Constraint getConstraint() {
        return this.constraint;
    }

    @Override
    public void startVisit(final Query query) {
        LOGGER.trace("START VISIT: {}", query);
        this.columns = new ImmutableSet.Builder<>();
        this.orderings = new ImmutableList.Builder<>();
    }

    @Override
    public void visitPrologue(final Prologue prologue) {
        LOGGER.trace("VISIT PROLOGUE: {}", prologue);
    }

    @Override
    public void visitResultForm(final Query query) {
        LOGGER.trace("VISIT RESULT FORM: {}", query);
    }

    @Override
    public void visitSelectResultForm(final Query query) {
        LOGGER.trace("VISIT SELECT RESULT FORM: {}", query.getResultVars());
        resultsVars = Collections.unmodifiableSet(query.getResultVars());

        this.distinct = query.isDistinct();
    }

    @Override
    public void visitConstructResultForm(final Query query) {
        LOGGER.trace("VISIT CONSTRUCT RESULT FORM: {}", query);
    }

    @Override
    public void visitDescribeResultForm(final Query query) {
        LOGGER.trace("VISIT DESCRIBE RESULT FORM: {}", query);
    }

    @Override
    public void visitAskResultForm(final Query query) {
        LOGGER.trace("VISIT ASK RESULT FORM: {}", query);
    }

    @Override
    public void visitDatasetDecl(final Query query) {
        if (query.hasDatasetDescription()) {
            LOGGER.trace("VISIT DATASET DESC FORM: {}", query.getDatasetDescription());
        }
    }

    @Override
    public void visitQueryPattern(final Query query) {
        LOGGER.trace("VISIT QUERY PATTERN: {}", query.getQueryPattern());
        final Element queryPattern = query.getQueryPattern();
        queryPattern.visit(this);
    }

    @Override
    public void visitGroupBy(final Query query) {
        if (query.hasGroupBy()) {
            LOGGER.trace("VISIT GROUP BY: {}", query.getGroupBy());
            throw new NotImplementedException("GROUP BY");
        }
    }

    @Override
    public void visitHaving(final Query query) {
        if (query.hasHaving()) {
            LOGGER.trace("VISIT HAVING: {}", query.getHavingExprs());
            throw new NotImplementedException("HAVING");
        }
    }

    @Override
    public void visitOrderBy(final Query query) {
        if (query.hasOrderBy()) {
            LOGGER.trace("VISIT ORDER BY: {}", query.getOrderBy());
            try {
                for (final SortCondition sortCondition : query.getOrderBy()) {

                    final PropertyValue property;
                    final Expr expression = sortCondition.getExpression();

                    if (expression.isConstant()) {
                        property = queryFactory.propertyValue(FEDORA_RESOURCE, expression.getConstant().asString());
                    } else if (expression.isVariable()) {
                        final Column c = variables.get(expression.getVarName());

                        property = queryFactory.propertyValue(c.getSelectorName(), c.getPropertyName());
                    } else {
                        property = null;
                    }

                    if (property != null) {
                        final Ordering ordering;

                        if (sortCondition.getDirection() == ORDER_DESCENDING) {
                            ordering = queryFactory.descending(property);
                        } else {
                            ordering = queryFactory.ascending(property);

                        }

                        this.orderings.add(ordering);
                    } else {
                        LOGGER.debug("IGNORING UNKNOWN ORDER CONDITION {}", sortCondition);
                    }
                }

            } catch (final RepositoryException e) {
                throw propagate(e);
            }
        }
    }

    @Override
    public void visitLimit(final Query query) {
        if (query.hasLimit()) {
            LOGGER.trace("VISIT LIMIT: {}", query.getLimit());
            this.hasLimit = true;
            this.limit = query.getLimit();
        }
    }

    @Override
    public void visitOffset(final Query query) {
        if (query.hasOffset()) {
            LOGGER.trace("VISIT OFFSET: {}", query.getOffset());
            this.offset = query.getOffset();
        }
    }

    @Override
    public void visitValues(final Query query) {
        if (query.hasValues()) {
            LOGGER.trace("VISIT VALUES: {}", query.getValuesData());
            throw new NotImplementedException("VALUES");
        }
    }

    @Override
    public void finishVisit(final Query query) {
        LOGGER.trace("FINISH VISIT: {}", query);
    }

    @Override
    public void visit(final ElementTriplesBlock el) {
        LOGGER.trace("VISIT TRIPLES: {}", el);
        final Iterator<Triple> tripleIterator = el.patternElts();

        while (tripleIterator.hasNext()) {
            final Triple next = tripleIterator.next();
            next.getObject();
        }
    }

    @Override
    public void visit(final ElementPathBlock el) {
        LOGGER.trace("VISIT PATH BLOCK: {}", el);
        Iterator<TriplePath> triplePathIterator = el.patternElts();


        try {
            while (triplePathIterator.hasNext()) {
                final TriplePath next = triplePathIterator.next();
                final Node subject = next.getSubject();

                if (subject.isVariable()) {
                    final String selectorName =
                        "fedoraResource_" + subject.getName();

                    this.joins.put(subject.getName(), queryFactory.selector(
                            FEDORA_RESOURCE, selectorName));

                    final Column c =
                        queryFactory.column(selectorName, JCR_PATH, subject
                                .getName());
                    variables.put(subject.getName(), c);
                }
            }

            triplePathIterator = el.patternElts();

            while (triplePathIterator.hasNext()) {
                final TriplePath next = triplePathIterator.next();
                LOGGER.trace(" - TRIPLE PATH: {}", next);

                final Node subject = next.getSubject();
                final Node predicate = next.getPredicate();
                final Node object = next.getObject();
                final Model defaultModel = createDefaultModel();

                if (subject.isVariable()) {
                    final Column c = variables.get(subject.getName());

                    if (resultsVars.contains(subject.getName())) {
                        columns.add(c);
                    }

                    if (predicate.isVariable()) {
                        throw new NotImplementedException(
                                "Element path may not contain a variable predicate");
                    }

                    final String propertyName =
                        jcrTools.getPropertyNameFromPredicate(defaultModel
                                .createProperty(predicate.getURI()));

                    if (propertyName.equals("rdf:type") && object.isURI()) {
                        final String mixinName =
                            jcrTools.getPropertyNameFromPredicate(defaultModel
                                    .createProperty(object.getURI()));

                        if (session.getWorkspace().getNodeTypeManager()
                                .hasNodeType(mixinName)) {
                            final String selectorName =
                                "ref_type_" + mixinName.replace(":", "_");

                            this.joins.put(selectorName, queryFactory.selector(
                                    mixinName, selectorName));

                            joinTypes.put(selectorName, JCR_JOIN_TYPE_INNER);
                            joinConditions.put(selectorName, queryFactory
                                    .sameNodeJoinCondition(c.getSelectorName(),
                                            selectorName, "."));
                            continue;
                        }
                    }

                    final int propertyType = jcrTools.getPropertyType(FEDORA_RESOURCE, propertyName);

                    if (object.isVariable()) {

                        final Column objectColumn;

                        if ((propertyType == REFERENCE || propertyType == WEAKREFERENCE || propertyType == URI)
                                && variables.containsKey(object.getName()))  {

                            objectColumn = variables.get(object.getName());

                            final String joinPropertyName;

                            if (propertyType == URI) {
                                joinPropertyName =
                                    propertiesTools
                                            .getReferencePropertyName(propertyName);
                            } else {
                                joinPropertyName = propertyName;
                            }

                            joinConditions.put(object.getName(),
                                                  queryFactory.equiJoinCondition(
                                                      c.getSelectorName(), joinPropertyName,
                                                      objectColumn.getSelectorName(), "jcr:uuid"));
                        } else {
                            objectColumn = queryFactory.column(c.getSelectorName(),
                                                                  propertyName,
                                                                  object.getName());

                            variables.put(object.getName(), objectColumn);

                            if (resultsVars.contains(object.getName())) {
                                columns.add(objectColumn);
                            }
                        }

                        if (!inOptional) {
                            appendConstraint(queryFactory.propertyExistence(c.getSelectorName(), propertyName));
                        }
                    } else {

                        if (!inOptional) {
                            final PropertyValue field = queryFactory.propertyValue(c.getSelectorName(), propertyName);
                            final Value jcrValue = jcrTools.createValue(defaultModel.asRDFNode(object), propertyType);
                            final Literal literal = queryFactory.literal(jcrValue);
                            appendConstraint(queryFactory.comparison(field, JCR_OPERATOR_EQUAL_TO, literal));
                        }

                    }

                } else if (predicate.isVariable()) {
                    throw new NotImplementedException("Element path with constant subject and variable predicate");

                } else if (object.isVariable()) {
                    throw new NotImplementedException(
                            "Element path with constant subject and predicate, and a variable object");

                } else {
                    throw new NotImplementedException("Element path with constant subject/predicate/object");
                }

            }
        } catch (final RepositoryException e) {
            throw propagate(e);
        }
    }

    @Override
    public void visit(final ElementFilter el) {
        LOGGER.trace("VISIT FILTER: {}", el);
        el.getExpr().visit(this);
    }

    @Override
    public void visit(final ElementAssign el) {
        LOGGER.trace("VISIT ASSIGN: {}", el);
        throw new NotImplementedException("ASSIGN");
    }

    @Override
    public void visit(final ElementBind el) {
        LOGGER.trace("VISIT BIND: {}", el);
        throw new NotImplementedException("BIND");
    }

    @Override
    public void visit(final ElementData el) {
        LOGGER.trace("VISIT DATA: {}", el);
        throw new NotImplementedException("DATA");
    }

    @Override
    public void visit(final ElementUnion el) {
        LOGGER.trace("VISIT UNION: {}", el);
        throw new NotImplementedException("UNION");
    }

    @Override
    public void visit(final ElementOptional el) {
        LOGGER.trace("VISIT OPTIONAL: {}", el);
        this.inOptional = true;
        el.getOptionalElement().visit(this);
        this.inOptional = false;
    }

    @Override
    public void visit(final ElementGroup el) {
        LOGGER.trace("VISIT GROUP: {}", el);

        for (final Element element : el.getElements()) {
            element.visit(this);
        }
    }

    @Override
    public void visit(final ElementDataset el) {
        LOGGER.trace("VISIT DATASET: {}", el);
        throw new NotImplementedException("DATASET");
    }

    @Override
    public void visit(final ElementNamedGraph el) {
        LOGGER.trace("VISIT NAMED GRAPH: {}", el);
        throw new NotImplementedException("NAMED GRAPH");
    }

    @Override
    public void visit(final ElementExists el) {
        LOGGER.trace("VISIT EXISTS: {}", el);
        throw new NotImplementedException("EXISTS");
    }

    @Override
    public void visit(final ElementNotExists el) {
        LOGGER.trace("VISIT NOT EXISTS: {}", el);
        throw new NotImplementedException("NOT EXISTS");
    }

    @Override
    public void visit(final ElementMinus el) {
        LOGGER.trace("VISIT MINUS: {}", el);
        throw new NotImplementedException("MINUS");
    }

    @Override
    public void visit(final ElementService el) {
        LOGGER.trace("VISIT SERVICE: {}", el);
        throw new NotImplementedException("SERVICE");
    }

    @Override
    public void visit(final ElementSubQuery el) {
        LOGGER.trace("VISIT SUBQUERY: {}", el);
        throw new NotImplementedException("SUB QUERY");
    }

    @Override
    public void startVisit() {
    }

    @Override
    public void visit(final ExprFunction0 func) {
        LOGGER.trace("VISIT EXPRFUNCTION0: {}", func);
    }

    @Override
    public void visit(final ExprFunction1 func) {
        LOGGER.trace("VISIT EXPRFUNCTION1: {}", func);
        final String funcName = func.getFunctionSymbol().getSymbol().toLowerCase();

        try {
            switch (funcName) {
                case "not":
                    final JQLQueryVisitor subVisitor1 = new JQLQueryVisitor(this);
                    func.getArg().visit(subVisitor1);
                    appendConstraint(queryFactory.not(subVisitor1.getConstraint()));
                    break;
                case "bound":
                    final Column column = variables.get(func.getArg().getVarName());
                    appendConstraint(queryFactory.propertyExistence(column.getSelectorName(),
                                                                    column.getPropertyName()));
                    break;
                default:
                    throw new NotImplementedException(funcName);
            }

        } catch (final RepositoryException e) {
            LOGGER.info(e.getMessage());
        }
    }

    @Override
    public void visit(final ExprFunction2 func) {
        LOGGER.trace("VISIT EXPRFUNCTION2: {}", func);
        final String funcName = func.getFunctionSymbol().getSymbol().toLowerCase();

        try {
            if (funcName.equals("and") || funcName.equals("or")) {
                final JQLQueryVisitor subVisitor1 = new JQLQueryVisitor(this);
                func.getArg1().visit(subVisitor1);


                final JQLQueryVisitor subVisitor2 = new JQLQueryVisitor(this);
                func.getArg2().visit(subVisitor2);

                switch (funcName) {
                    case "and":
                        appendConstraint(queryFactory.and(subVisitor1
                                .getConstraint(), subVisitor2.getConstraint()));
                        break;
                    case "or":
                        appendConstraint(queryFactory.or(subVisitor1
                                .getConstraint(), subVisitor2.getConstraint()));
                        break;
                    default:
                        throw new NotImplementedException(funcName);
                }
            } else if (!func.getArg2().isConstant()) {
                throw new NotImplementedException(
                        "EXPRFUNCTION2 2nd argument must be a constant: "
                                + func.getArg1() + "; " + func.getArg2());
            } else {
                final String op;
                String value = func.getArg2().getConstant().getString();
                switch(funcName) {
                    case "eq":
                        op = JCR_OPERATOR_EQUAL_TO;
                        break;
                    case "ge":
                        op = JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO;
                        break;
                    case "le":
                        op = JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO;
                        break;
                    case "lt":
                        op = JCR_OPERATOR_LESS_THAN;
                        break;
                    case "gt":
                        op = JCR_OPERATOR_GREATER_THAN;
                        break;
                    case "ne":
                        op = JCR_OPERATOR_NOT_EQUAL_TO;
                        break;
                    case "contains":
                        op = JCR_OPERATOR_LIKE;
                        value = "%" + value + "%";
                        break;
                    case "strstarts":
                        op = JCR_OPERATOR_LIKE;
                        value = value + "%";
                        break;
                    case "strends":
                        op = JCR_OPERATOR_LIKE;
                        value = "%" + value;
                        break;
                    default:
                        throw new NotImplementedException(funcName);
                }

                appendConstraint(queryFactory.comparison(getPropertyValue(func
                        .getArg1()), op, queryFactory.literal(getValue(value))));

            }

        } catch (final RepositoryException e) {
            throw propagate(e);
        }


    }

    @Override
    public void visit(final ExprFunction3 func) {
        LOGGER.trace("VISIT EXPRFUNCTION3: {}", func);
    }

    @Override
    public void visit(final ExprFunctionN func) {
        LOGGER.trace("VISIT EXPRFUNCTIONN: {}", func);
        try {
            final FunctionLabel functionSymbol = func.getFunctionSymbol();
            final List<Expr> args = func.getArgs();

            final String symbol = functionSymbol.getSymbol().toLowerCase();
            if (symbol.equals("regex")) {
                final Expr expr = args.get(0);

                if (expr.isVariable()) {
                    appendConstraint(queryFactory.comparison(
                            getPropertyValue(expr), JCR_OPERATOR_LIKE,
                            queryFactory.literal(getValue(args.get(1)))));
                } else {
                    throw new NotImplementedException("ExprFunctionN " + symbol);
                }

            } else {
                throw new NotImplementedException("ExprFunctionN " + symbol);
            }

        } catch (final RepositoryException e) {
            throw propagate(e);
        }
    }

    @Override
    public void visit(final ExprFunctionOp funcOp) {
        LOGGER.trace("VISIT EXPRFUNCTIONOp: {}", funcOp);
    }

    @Override
    public void visit(final NodeValue nv) {
        LOGGER.trace("VISIT NODEVALUE: {}", nv);
    }

    @Override
    public void visit(final ExprVar nv) {
        LOGGER.trace("VISIT EXPRVAR: {}", nv);
    }

    @Override
    public void visit(final ExprAggregator eAgg) {
        LOGGER.trace("VISIT EXPRAGGREGATOR: {}", eAgg);
    }

    @Override
    public void finishVisit() {
    }

    private PropertyValue getPropertyValue(final Column column) {
        try {
            return queryFactory.propertyValue(column.getSelectorName(), column.getPropertyName());
        } catch (final RepositoryException e) {
            throw propagate(e);
        }
    }

    private PropertyValue getPropertyValue(final Expr expr) {
        return getPropertyValue(variables.get(expr.getVarName()));
    }

    private Value getValue(final Expr e) throws RepositoryException {
        return getValue(e.getConstant().asString());
    }

    private Value getValue(final String e) throws RepositoryException {
        return session.getValueFactory().createValue(e);
    }

    private void appendConstraint(final Constraint c) throws RepositoryException {
        if (constraint == null) {
            constraint = c;
        } else {
            constraint = queryFactory.and(constraint, c);
        }
    }
}
