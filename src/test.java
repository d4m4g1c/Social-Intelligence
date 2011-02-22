import java.io.IOException;
import java.util.LinkedHashMap;

import javax.xml.soap.SOAPException;


public class test {
	public static void main(String[] args) {
		new LinkedHashMap () {
			@Override
			protected boolean removeEldestEntry(java.util.Map.Entry eldest) {
				// TODO Auto-generated method stub
				return super.removeEldestEntry(eldest);
			}
			
		};
		for (int i=33; i<34; i++) {
			try {
				Neolane4J myNeolane = Neolane4J.getInstance();
				myNeolane.select("ecp", "results",
						new String[] {"id", "source", "datetime", "link"});
			} catch (SOAPException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}//log4J
//Jfreechart
//googlecharts