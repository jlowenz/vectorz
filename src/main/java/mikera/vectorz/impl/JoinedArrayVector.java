package mikera.vectorz.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mikera.vectorz.AVector;
import mikera.vectorz.ArrayVector;
import mikera.vectorz.Op;
import mikera.vectorz.Vectorz;
import mikera.vectorz.util.DoubleArrays;
import mikera.vectorz.util.VectorzException;

/**
 * Class representing a join of one or more array subvectors
 * @author Mike
 *
 */
public final class JoinedArrayVector extends AVector {
	private static final long serialVersionUID = -8470277860344236392L;

	private final int length; // total length
	private final int numArrays; // number of joined arrays
	private final double[][] data; // source arrays
	private final int[] offsets; // offsets into source arrays
	private final int[] pos; // position of each array within vector. contains one extra element
	
	private JoinedArrayVector(int length, double[][] newData,int[] offsets, int[] pos) {
		this.length=length;
		this.numArrays=newData.length;
		this.offsets=offsets;
		this.pos=pos;
		this.data=newData;
	}
	
	public static final JoinedArrayVector create(AVector v) {
		int length=v.length();
		double[][] data=new double[1][];
		data[0]=new double[length];
		v.copyTo(data[0], 0);
		JoinedArrayVector jav=new JoinedArrayVector(length,data,new int[1],new int[] {0,length});
		return jav;
	}
	
	public static JoinedArrayVector wrap(ArrayVector v) {
		return new JoinedArrayVector(v.length(),
				new double[][] {v.getArray()},
				new int[] {v.getArrayOffset()},
				new int[] {0,v.length()});
	}
	
	public int numArrays() {
		return this.numArrays;
	}

	// finds the number of the array that contains a specific index position
	private int findArrayNum(int index) {
		assert((index>=0)&&(index<length));
		int i=0;
		int j=numArrays-1;
		while (i<j) {
			int m=(i+j)>>1;
			int p=pos[m];
			if (index<p) {j=m; continue;}
			int p2=pos[m+1];
			if (index>=p2) {i=m+1; continue;}
			return m;
		}
		return i;
	}
	
	private int subLength(int j) {
		return pos[j+1]-pos[j];
	}
	
	private ArraySubVector subArrayVector(int j) {
		return ArraySubVector.wrap(data[j], offsets[j], subLength(j));
	}
	
	public List<ArrayVector> toSubArrays() {
		ArrayList<ArrayVector> al=new ArrayList<ArrayVector>();
		for (int i=0; i<numArrays; i++) {
			al.add(subArrayVector(i));
		}
		return al;
	}
	
	@Override
	public int length() {
		return length;
	}
	
	@Override
	public boolean isView() {
		return true;
	}
	
	@Override
	public boolean isFullyMutable() {
		return true;
	}

	@Override
	public double get(int i) {
		int ai=findArrayNum(i);
		return data[ai][i-pos[ai]+offsets[ai]];
	}

	@Override
	public void set(int i, double value) {
		if ((i<0)||(i>=length)) throw new IndexOutOfBoundsException("Index: "+i);
		int ai=findArrayNum(i);
		data[ai][i-pos[ai]+offsets[ai]]=value;
	}
	
	@Override
	public void addAt(int i, double value) {
		int ai=findArrayNum(i);
		data[ai][i-pos[ai]+offsets[ai]]+=value;
	}
	
	@Override
	public void copyTo(AVector dest, int offset) {
		for (int j=0; j<numArrays; j++) {
			dest.set(pos[j]+offset,data[j],offsets[j],subLength(j));
		}
	}

	@Override
	public double elementSum() {
		double result=0.0;
		for (int j=0; j<numArrays; j++) {
			result+=DoubleArrays.elementSum(data[j], offsets[j], subLength(j));
		}
		return result;
	}
	
	@Override
	public long nonZeroCount() {
		long result=0;
		for (int j=0; j<numArrays; j++) {
			result+=DoubleArrays.nonZeroCount(data[j], offsets[j], subLength(j));
		}
		return result;
	}
	
	@Override
	public double dotProduct (AVector v) {
		if (v instanceof ArrayVector) {
			ArrayVector av=(ArrayVector)v;
			return dotProduct(av);
		}
		return super.dotProduct(v);
	}
	
	public double dotProduct (ArrayVector v) {
		double result=0.0;
		double[] arr=v.getArray();
		int ao=v.getArrayOffset();
		for (int j=0; j<numArrays; j++) {
			result+=DoubleArrays.dotProduct(data[j], offsets[j], arr,ao+pos[j],subLength(j));
		}
		return result;
	}
	
	@Override
	public void add(AVector a) {
		add(0,a,0,length);
	}
	
	@Override
	public void add(int offset, AVector a) {
		add(offset,a,0,a.length());
	}
	
	@Override
	public void add(int offset, AVector a, int aOffset, int length) {
		int alen=length;
		for (int j=0; j<numArrays; j++) {
			if (offset>=pos[j+1]) continue; // skip until adding at right place
			int segmentOffset=Math.max(0,offset-pos[j]);
			int len=Math.min(subLength(j)-segmentOffset, offset+alen-pos[j]);
			if (len>0) {
				a.addToArray(aOffset+pos[j]+segmentOffset-offset, data[j], offsets[j]+segmentOffset, len);
			}
		}
	}
	
	@Override
	public void addProduct(AVector a, AVector b, double factor) {
		addProduct(a,0,b,0,factor);
	}
	
	@Override
	public void addMultiple(AVector a, double factor) {
		addMultiple(0,a,0,length(),factor);
	}
	
	@Override
	public void addMultiple(int offset, AVector a,double factor) {
		addMultiple(offset,a,0,a.length(),factor);
	}
	
	@Override
	public void addMultiple(int offset, AVector a, int aOffset, int length, double factor) {
		int alen=length;
		for (int j=0; j<numArrays; j++) {
			if (offset>=pos[j+1]) continue; // skip until adding at right place
			int segmentOffset=Math.max(0,offset-pos[j]);
			int len=Math.min(subLength(j)-segmentOffset, offset+alen-pos[j]);
			if (len>0) {
				a.addMultipleToArray(factor,aOffset+pos[j]+segmentOffset-offset, data[j], offsets[j]+segmentOffset, len);
			}
		}
	}
	
	@Override
	public void addMultipleToArray(double factor,int offset, double[] array, int arrayOffset, int length) {
		int startArray=findArrayNum(offset);
		int endPos=offset+length; // end poition in this vector
		for (int j=startArray, sp=pos[j]; sp<endPos; j++, sp=pos[j]) {
			int ep=pos[j+1];
			int clen=Math.min(ep-sp, endPos-sp);
			double[] sa=data[j];
			int soffset=offsets[j];
			if (offset<=sp) {
				// chunk start aligned with current array, so just use clen elements
				for (int i=0; i<clen; i++) {
					array[arrayOffset+i]+=factor*sa[soffset+i];
				}
			} else {
				// first chunk not starting at sp, need to skip some elements
				int skip=offset-sp;
				assert(skip>0);
				clen-=skip;
				for (int i=0; i<clen; i++) {
					array[arrayOffset+i]+=factor*sa[skip+soffset+i];
				}
			}
			arrayOffset+=clen;
		}
	}
	
	@Override
	public void addProduct(AVector a, int aOffset, AVector b, int bOffset, double factor) {
		for (int j=0; j<numArrays; j++) {
			a.addProductToArray(factor, aOffset+pos[j], b, bOffset+pos[j], data[j], offsets[j], subLength(j));
		}
	}
	
	@Override
	public void applyOp(Op op) {
		for (int j=0; j<numArrays; j++) {
			op.applyTo(data[j], offsets[j], subLength(j));
		}
	}
	
	@Override
	public void copyTo(double[] destArray, int offset) {
		for (int j=0; j<numArrays; j++) {
			System.arraycopy(this.data[j],offsets[j],destArray,offset+pos[j],subLength(j));
		}
	}
	
	@Override
	public void multiplyTo(double[] target, int offset) {
		for (int j=0; j<numArrays; j++) {
			DoubleArrays.arraymultiply(this.data[j],offsets[j],target,offset+pos[j],subLength(j));
		}
	}
	
	@Override
	public void divideTo(double[] target, int offset) {
		for (int j=0; j<numArrays; j++) {
			DoubleArrays.arraydivide(this.data[j],offsets[j],target,offset+pos[j],subLength(j));
		}
	}
	
	@Override 
	public void fill(double value) {
		for (int j=0; j<numArrays; j++) {
			Arrays.fill(this.data[j],offsets[j],offsets[j]+subLength(j),value);
		}
	}
	
	@Override 
	public void set(AVector v) {
		for (int j=0; j<numArrays; j++) {
			v.copyTo(pos[j],data[j], offsets[j],subLength(j));
		}
	}
	
	@Override
	public void setElements(double[] values, int offset, int length) {
		assert(length==this.length());
		int srcPos=offset;
		for (int j=0; j<numArrays; j++) {
			int sl=subLength(j);
			System.arraycopy(values,srcPos, data[j],offsets[j],sl);
			srcPos+=sl;
		}
	} 
	
	@Override 
	public void multiply(double value) {
		for (int j=0; j<numArrays; j++) {
			DoubleArrays.multiply(this.data[j],offsets[j],subLength(j),value);
		}
	}
	
	@Override
	public void square() {
		for (int j=0; j<numArrays; j++) {
			DoubleArrays.square(this.data[j],offsets[j],subLength(j));
		}		
	}

	@Override
	public JoinedArrayVector exactClone() {
		double[][] newData=new double[numArrays][];
		int[] zeroOffsets=new int[numArrays];
		for (int i=0; i<numArrays; i++) {
			int alen=subLength(i);
			double[] arr=new double[alen];
			newData[i]=arr;
			System.arraycopy(data[i], offsets[i], arr, 0, alen);
		}
		return new JoinedArrayVector(length,newData,zeroOffsets,pos);
	}
	
	@Override
	public AVector subVector(int start, int length) {
		assert(start>=0);
		assert((start+length)<=this.length);
		if (length==0) return Vector0.INSTANCE;
		
		int a=findArrayNum(start);
		int b=findArrayNum(start+length-1);
		int n=b-a+1;
		
		if (n==1) return Vectorz.wrap(data[a], start-pos[a], length);
		
		double[][] newData=Arrays.copyOfRange(data, a, b+1);
		int[] offs=new int[n];
		offs[0]=offsets[a]+(start-pos[a]);
		for (int j=1; j<n; j++) offs[j]=offsets[a+j];
		
		int[] poses=new int[n+1];
		poses[0]=0;
		for (int j=1; j<n; j++) poses[j]=pos[a+j]-start;
		poses[n]=length;
		
		return new JoinedArrayVector(length,newData,offs,poses);
	}
	
	@Override
	public AVector join(AVector v) {
		if (v instanceof JoinedArrayVector) return joinVectors(this,(JoinedArrayVector) v);
		if (v instanceof ArrayVector) return join((ArrayVector) v);
		return super.join(v);
	}
	
	public JoinedArrayVector join(ArrayVector v) {
		int newLen=length+v.length();
		
		int[] newOffsets=new int[numArrays+1];
		System.arraycopy(offsets, 0, newOffsets, 0, numArrays);
		newOffsets[numArrays]=v.getArrayOffset();
		
		int[] newPos=new int[numArrays+2];
		System.arraycopy(pos, 0, newPos, 0, numArrays+1);
		newPos[numArrays+1]=newLen;

		double[][] newData=new double[numArrays+1][];
		System.arraycopy(data, 0, newData, 0, numArrays);
		newData[numArrays]=v.getArray();
		
		return new JoinedArrayVector(newLen,newData,newOffsets,newPos);
	}
	
	public JoinedArrayVector join(JoinedArrayVector v) {
		return joinVectors(this,v);
	}
	
	public static JoinedArrayVector joinVectors(JoinedArrayVector a, JoinedArrayVector b) {
		int newLen=a.length+b.length();
		
		int naa=a.numArrays; int nab=b.numArrays;
		
		if ((a.data[naa-1]==b.data[0])&&(a.offsets[naa-1]+(a.pos[naa]-a.pos[naa-1])==b.offsets[0])) {
			// need to join array at end of a with start of b
			
			int[] newOffsets=new int[naa+nab-1];
			System.arraycopy(a.offsets, 0, newOffsets, 0, naa);
			System.arraycopy(b.offsets, 1, newOffsets, naa, nab-1);
			
			int[] newPos=new int[naa+nab-1+1];
			System.arraycopy(a.pos, 0, newPos, 0, naa);
			System.arraycopy(b.pos, 1, newPos, naa, nab);
			for (int i=naa; i<newPos.length; i++) {
				newPos[i]+=a.length;
			}
			
			double[][] newData=new double[naa+nab-1][];
			System.arraycopy(a.data, 0, newData, 0, naa);
			System.arraycopy(b.data, 1, newData, naa, nab-1);

			return new JoinedArrayVector(newLen,newData,newOffsets,newPos);
		} else {
			int[] newOffsets=new int[naa+nab];
			System.arraycopy(a.offsets, 0, newOffsets, 0, naa);
			System.arraycopy(b.offsets, 0, newOffsets, naa, nab);
			
			int[] newPos=new int[naa+nab+1];
			System.arraycopy(a.pos, 0, newPos, 0, naa);
			System.arraycopy(b.pos, 0, newPos, naa, nab+1);
			for (int i=naa; i<newPos.length; i++) {
				newPos[i]+=a.length;
			}
	
			double[][] newData=new double[naa+nab][];
			System.arraycopy(a.data, 0, newData, 0, naa);
			System.arraycopy(b.data, 0, newData, naa, nab);
			
			return new JoinedArrayVector(newLen,newData,newOffsets,newPos);
		}
	}

	public static AVector joinVectors(ArrayVector a, ArrayVector b) {
		if (a.getArray()==b.getArray()) {
			if ((a.getArrayOffset()+a.length())==b.getArrayOffset()) {
				return Vectorz.wrap(a.getArray(),a.getArrayOffset(),a.length()+b.length());
			}
		}
		
		int alen=a.length();
		int blen=b.length();
		return new JoinedArrayVector(
				alen+blen,
				new double[][] {a.getArray(),b.getArray()},
				new int[] {a.getArrayOffset(),b.getArrayOffset()},
				new int[] {0,alen,alen+blen});
	}
	
	@Override
	public void validate() {
		if (length!=pos[numArrays]) throw new VectorzException("Validation problem");
		super.validate();
	}
}
