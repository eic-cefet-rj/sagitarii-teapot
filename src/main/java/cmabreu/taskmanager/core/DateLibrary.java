package cmabreu.taskmanager.core;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateLibrary {
	private Calendar calendar;
	private SimpleDateFormat ft;
	private SimpleDateFormat fc;
	private SimpleDateFormat ftm;
	private SimpleDateFormat fm;
	private SimpleDateFormat sq;
	private static DateLibrary dl;
	
	public static DateLibrary getInstance() {
		if ( dl == null ) {
			dl = new DateLibrary();
		}
		return dl;
	}
	
	private DateLibrary() {
		Date dNow = new Date( );
		ft = new SimpleDateFormat ("dd/MM/yyyy");
		fc = new SimpleDateFormat ("dd 'de' MMMM 'de' yyyy");
		ftm = new SimpleDateFormat ("dd/MM/yyyy HH:mm:ss");
		fm = new SimpleDateFormat ("HH:mm:ss");
		sq = new SimpleDateFormat ("yyyy-MM-dd");
		calendar = Calendar.getInstance();
		calendar.setTime(dNow);
	}

	public Date getDateFromTime( String time ) throws ParseException {
		Date date = fm.parse( time );
		setTo(date);
		return date;
	}
	
	public void setToStartOfMonth() {
		calendar.set(Calendar.DAY_OF_MONTH, 1);		
	}

	public void setToStartOfYear() {
		calendar.set(Calendar.DAY_OF_YEAR, 1);		
	}
	
	
	public void increaseDay(int days){
		calendar.add(Calendar.DATE, days);
	}
	
	public long getDiferencaDiasAte(Calendar data) {
		long diferenca = 0;
		if (  isAfter(data)  ) {
			diferenca = calendar.getTimeInMillis() - data.getTimeInMillis();
		} else {
			diferenca = data.getTimeInMillis() - calendar.getTimeInMillis();
		}
		long segundosDiferenca = diferenca / (1000 * 60 * 60 * 24);    	
		return segundosDiferenca;    	
	}
	
	public long getDiferencaMilisAte(Calendar data) {
		long diferenca = 0;
		if (  isAfter(data)  ) {
			diferenca = calendar.getTimeInMillis() - data.getTimeInMillis();
		} else {
			diferenca = data.getTimeInMillis() - calendar.getTimeInMillis();
		}
		return diferenca;    	
	}
	
	public boolean isAfter( Calendar data ) {
		return ( calendar.after(data) );
	}

	public boolean isInBetween(Calendar dtInicial, Calendar dtFinal ) {
    	if (   (calendar.after(dtInicial)) && (calendar.before(dtFinal))  ){
    		return true;
    	}
		return false;		
	}

	public Calendar asCalendar() {
		return calendar;
	}
	
	public boolean isBefore( Calendar data ) {
		return ( calendar.before(data) );
	}

	
	public void setTo( Date data ) {
		if ( data != null ) {
			calendar.setTime(data);
		}
	}

	public Date asDate() {
		return calendar.getTime();
	}
	
	public String getDateTextSQL() {
		return ft.format( calendar.getTime() );
	}
	
	public String getDateTextHuman() {
		return ft.format( calendar.getTime() );
	}

	public String getHourTextHuman() {
		return fm.format( calendar.getTime() );
	}
	
	public String getDateHourTextHuman( Date input ) {
		setTo( input );
		return ftm.format( calendar.getTime() );
	}
	
	public String getDateHourTextHuman() {
		return ftm.format( calendar.getTime() );
	}

	public String getCompleteDateTextHuman( Date setToThis ) {
		setTo( setToThis );
		return fc.format( calendar.getTime() );
	}

	
	public String getCompleteDateTextHuman() {
		return fc.format( calendar.getTime() );
	}
	
	public void setDateTextHuman(String data) {
		try {
		    setTo( ft.parse(data) );
		} catch (ParseException ex) {
		    setTo( new Date( ) );
		}
	}

	public void setTimeTextHuman(String hora) {
		try {
		    setTo( fm.parse(hora) );
		} catch (ParseException ex) {
		    setTo( new Date( ) );
		}
	} 
	
	
	public void setDateTextSQL(String data) {
		try {
		    setTo( sq.parse(data) );
		} catch (ParseException ex) {
		    setTo( new Date( ) );
		}
	}
	
	
}
