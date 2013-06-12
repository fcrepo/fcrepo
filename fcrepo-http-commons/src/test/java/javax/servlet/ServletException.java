
package javax.servlet;

@SuppressWarnings("serial")
public class ServletException extends Exception {

    public ServletException(final String mesg) {
        super(mesg);
    }

    public ServletException(final Throwable e) {
        super(e);
    }

}
