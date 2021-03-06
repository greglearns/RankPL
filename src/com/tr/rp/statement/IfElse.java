package com.tr.rp.statement;

import java.util.LinkedHashSet;

import com.tr.rp.core.DStatement;
import com.tr.rp.core.Rank;
import com.tr.rp.core.VarStore;
import com.tr.rp.core.rankediterators.AbsurdIterator;
import com.tr.rp.core.rankediterators.BufferingIterator;
import com.tr.rp.core.rankediterators.IteratorSplitter;
import com.tr.rp.core.rankediterators.MergingIterator;
import com.tr.rp.core.rankediterators.RankTransformIterator;
import com.tr.rp.core.rankediterators.RankedIterator;
import com.tr.rp.expressions.bool.BoolExpression;
import com.tr.rp.expressions.bool.Not;
import com.tr.rp.expressions.num.NumExpression;
import com.tr.rp.tools.ResultPrinter;

public class IfElse implements DStatement {

	private BoolExpression exp;
	private DStatement a, b;
	
	public IfElse(BoolExpression exp, DStatement a, DStatement b) {
		this.exp = exp;
		this.a = a;
		this.b = b;
	}

	public IfElse(BoolExpression exp, DStatement a) {
		this(exp, a, new Skip());
	}

	@Override
	public RankedIterator getIterator(RankedIterator parent) {
		
		// If exp is contradiction/tautology we
		// can immediately pass the a/b iterator.
		if (exp.isTautology()) {
			return a.getIterator(parent);
		}
		if (exp.isContradiction()) {
			return b.getIterator(parent);
		}
		
		// Replace rank expressions in exp
		RankTransformIterator<BoolExpression> i = 
				new RankTransformIterator<BoolExpression>(parent, this.exp);
		BoolExpression exp2 = i.getExpression(0);
		
		// Check contradiction/tautology again.
		if (exp2.isTautology()) {
			return a.getIterator(i);
		}
		if (exp2.isContradiction()) {
			return b.getIterator(i);
		}	
		
		// Split input
		IteratorSplitter split = new IteratorSplitter(i);

		// Apply condition 
		RankedIterator ia1 = new Observe(exp2).getIterator(split.getA());
		RankedIterator ia2 = new Observe(exp2.negate()).getIterator(split.getB());
		
		// Remember offsets (prior ranks of the conditions)
		int offset1 = ia1.getConditioningOffset();
		int offset2 = ia2.getConditioningOffset();

		// Following happens if input iterator is empty
		if (offset1 == Integer.MAX_VALUE && offset2 == Integer.MAX_VALUE) {
			return new AbsurdIterator();
		}
		
		// Execute statements
		RankedIterator ib1 = a.getIterator(ia1);
		RankedIterator ib2 = b.getIterator(ia2);

		// Merge result
		RankedIterator rc = new MergingIterator(ib1, ib2, offset1, offset2);
		return rc;
	}

	public String toString() {
		return "if " + exp + " then " + a + " else " + b;
	}
	
	public boolean equals(Object o) {
		return o instanceof IfElse &&
				((IfElse)o).a.equals(a) &&
				((IfElse)o).b.equals(b) &&
				((IfElse)o).exp.equals(exp);
	}
}
