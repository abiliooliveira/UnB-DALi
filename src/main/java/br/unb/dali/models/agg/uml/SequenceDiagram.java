package br.unb.dali.models.agg.uml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import agg.xt_basis.Arc;
import agg.xt_basis.Graph;
import agg.xt_basis.Node;
import br.unb.dali.models.agg.AbstractAggModel;
import br.unb.dali.models.agg.exceptions.AggModelConstructionException;
import br.unb.dali.models.agg.exceptions.ModelSemanticsVerificationException;
import br.unb.dali.models.agg.markovchains.MultiDTMC;
import br.unb.dali.models.agg.uml.sd.Lifeline;
import br.unb.dali.models.agg.uml.sd.Occurrence;
import br.unb.dali.models.agg.uml.sd.messages.AsyncMessage;
import br.unb.dali.models.agg.uml.sd.occurrences.Event;
import br.unb.dali.models.agg.uml.sd.relations.First;
import br.unb.dali.models.agg.uml.sd.relations.Next;
import br.unb.dali.models.agg.uml.sd.relations.Receive;
import br.unb.dali.models.agg.uml.sd.relations.Send;
import br.unb.dali.transformations.agg.SD2DTMC;
import br.unb.dali.util.io.IOHelper;

/**
 * This class represents the UML Sequence Diagram Model for Dependability Analysis.
 * It's underlying infrastructure relies on the AGG Engine.
 * 
 * @author abiliooliveira
 */
public class SequenceDiagram extends AbstractAggModel {
	private static final String _grammar = "/models/SD.ggx";
	private List<Lifeline> _lifelines;
	private List<AsyncMessage> _messages;
	private String _name;
	
	/**
	 * Constructs a new empty Sequence Diagram.
	 * 
	 * @param id the identification string of this Model
	 * @param name the name of this UML 2.5 Sequence Diagram
	 * 
	 * @throws AggModelConstructionException (impossible with this constructor)
	 */
	public SequenceDiagram(String id, String name) throws AggModelConstructionException {
		super(id, null, _grammar);
		_name = name;
	}
	
	/**
	 * Constructs a Sequence Diagram from an AGG Graph object
	 *  
	 * @param graph the AGG Graph object
	 * @throws AggModelConstructionException 
	 * 	1 - if any of the objects of this Graph is not compatible with the type graph of this Model;
	 * 	2 - if syntactical errors are found;
	 * 	3 - if mandatory attributes are missing
	 */
	public SequenceDiagram(Graph graph) throws AggModelConstructionException {
		super(IOHelper.getRandomString(), graph, _grammar);
		// TODO Auto-generated constructor stub
	}

	/**************************** INHERITANCE BEHAVIOR *******************************/
	
	/**
	 * Checks the syntactic and semantic integrity of this Model.
	 * 
	 * @throws ModelSemanticsVerificationException if this model is NOT well formed
	 */
	public void checkModel() throws ModelSemanticsVerificationException {
		super.checkModel();
		// TODO: semantic verification
	}

	@Override
	protected void setUp() throws AggModelConstructionException {
		_lifelines = new ArrayList<Lifeline>();
		_messages = new ArrayList<AsyncMessage>();
		try {
			// set the nodes up
			if (setNodesUp(_graph.getNodesSet())) {
				// set the arcs up
				setArcsUp(_graph.getArcsSet());
			}
		} catch (Exception e) {
			throw new AggModelConstructionException(e);
		}
	}
	
	/**************************** PUBLIC BEHAVIOR *******************************/
	
	/**
	 * Transforms this UML Sequence Diagram to a DTMC with multiple Initial States.
	 * Each initial state refers to a Lifeline of this Sequence Diagram.
	 * 
	 * The name of the resulting model will be the same as this sequence diagram
	 * @return The corresponding DTMC of this Sequence Diagram
	 */
	public MultiDTMC toDTMC() {
		return new SD2DTMC(this).transform().setName(this._name);
	}
	
	/**
	 * Transforms this UML Sequence Diagram to a DTMC with multiple Initial States.
	 * Each initial state refers to a Lifeline of this Sequence Diagram.
	 * 
	 * @return The corresponding DTMC of this Sequence Diagram
	 */
	public MultiDTMC toDTMC(String name) {
		return new SD2DTMC(this).transform().setName(name);
	}
	
	/**
	 * Adds a new lifeline to this Sequence Diagram
	 * @param l the lifeline to be added
	 * @return this
	 */
	public SequenceDiagram addLifeline(Lifeline l) {
		this._lifelines.add(l);
		this.addAnAggNode(l);
		return this;
	}
	
	/**
	 * Adds an async message between two lifelines.
	 * 
	 * @param sourceLifelineId the string identifier of the sending lifeline
	 * @param targetLifelineId the string identifier of the receiving lifeline
	 * @param signal the message's signal
	 * @return this
	 */
	public AsyncMessage addAsyncMessage(String messageId,String sourceLifelineId, String targetLifelineId, String signal) {
		Lifeline source = (Lifeline) searchNode(sourceLifelineId);
		Lifeline target = (Lifeline) searchNode(targetLifelineId);
		return addAsyncMessage(messageId, source, target, signal);
	}
	
	/**
	 * Adds an async message between two lifelines.
	 * 
	 * @param source the lifeline that sends the message
	 * @param target the lifeline that receives the message
	 * @param signal the message's signal
	 * @return this
	 */
	public AsyncMessage addAsyncMessage(String messageId, Lifeline source, Lifeline target, String signal) {
		Event send = new Event(this);
		Event receive = new Event(this);
		AsyncMessage toReturn = addMessage(new AsyncMessage(messageId, signal, this).setSendAndReceive(send, receive));
		
		addAnAggNode(send); addAnAggNode(receive); addAnAggNode(toReturn);
		addOccurrenceToLifeline(source, send);
		addOccurrenceToLifeline(target, receive);
		addSendRelation(toReturn, send);
		addReceiveRelation(toReturn, receive);
		
		return toReturn;
	}
	
	
	/**************************** PRIVATE BEHAVIOR *******************************/
	
	/**
	 * Adds to the underlying graph the relationship between a lifeline and a new occurrence
	 * @param lifeline
	 * @param occ
	 */
	private void addOccurrenceToLifeline(Lifeline lifeline, Occurrence occ) {
		if (lifeline.getOccurrences().isEmpty()) {
			addAnAggEdge(new First(lifeline, occ, this));
		} else {
			Occurrence previous = lifeline.getLastOccurrence();
			previous.setNextOccurrence(occ);
			occ.setPreviousOccurrence(previous);
			addAnAggEdge(new Next(previous, occ, this));
		}
		lifeline.addOccurrence(occ);
		occ.setLifeline(lifeline);
	}
	
	/**
	 * Adds to the underlying graph the relationship between a lifeline send event and a message
	 * 
	 * @param m the message
	 * @param e the lifeline send event
	 */
	private void addSendRelation(AsyncMessage m, Event e) {
		addAnAggEdge(new Send(m, e, this));
	}
	
	/**
	 * Adds to the underlying graph the relationship between a lifeline receive event and a message
	 * 
	 * @param m the message
	 * @param e the lifeline receive event
	 */
	private void addReceiveRelation(AsyncMessage m, Event e) {
		addAnAggEdge(new Receive(m, e, this));
	}
	
	/**
	 * Adds a new message object to this sequence diagram
	 * @param toAdd the message to be added
	 * @return the added message
	 */
	private AsyncMessage addMessage(AsyncMessage toAdd) {
		_messages.add(toAdd);
		return toAdd;
	}
	
	private boolean setNodesUp(HashSet<Node> nodesSet) {
		// TODO Auto-generated method stub
		return false;
	}

	private void setArcsUp(HashSet<Arc> arcsSet) {
		// TODO Auto-generated method stub
	}

	
}
