package mikera.matrixx.performance;

import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;

import mikera.matrixx.Matrix;
import mikera.matrixx.Matrixx;
import mikera.matrixx.algo.Multiplications;
import mikera.vectorz.Op;
import mikera.vectorz.Ops;
import mikera.vectorz.Vector;
import mikera.vectorz.ops.Linear;

/**
 * Caliper based benchmarks
 * 
 * @author Mike
 */

public class ArrayPerfBenchmark extends SimpleBenchmark {
	public static final int MATRIX_SIZE=10;

	public void timeMutable(int runs) {
		Vector v=Vector.createLength(8388608);
		
		for (int i=0; i<runs; i++) {
			v.add(0.125);
			v.sqrt();
			v.scale(2.0);
		}		
	}
	
	public void timeImmutable(int runs) {
		Vector v=Vector.createLength(8388608);
		
		for (int i=0; i<runs; i++) {
			Vector a=(Vector) v.addCopy(0.375);
			a=(Vector) a.sqrtCopy();
			a=(Vector) a.scaleCopy(2.0);
		}		
	}
	
	@SuppressWarnings("unused")
	public void timeOpsImmutable(int runs) {
		Vector v=Vector.createLength(8388608);
		Op op = Ops.compose(Linear.create(2.0,0.0), Ops.compose(Ops.SQRT, Linear.create(0.0,0.375)));
		for (int i=0; i<runs; i++) {
			Vector a=(Vector) v.applyOpCopy(op);
		}		
	}
	
	public void timeOpsMutable(int runs) {
		Vector v=Vector.createLength(8388608);
		Op op = Ops.compose(Linear.create(2.0,0.0), Ops.compose(Ops.SQRT, Linear.create(0.0,0.375)));
		for (int i=0; i<runs; i++) {
			v.applyOp(op);
		}		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new ArrayPerfBenchmark().run();
	}

	private void run() {
		Runner runner=new Runner();
		runner.run(new String[] {this.getClass().getCanonicalName()});
	}

}
