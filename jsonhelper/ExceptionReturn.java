package jsonhelper;

public class ExceptionReturn {
    public String exception_type;
    public String exception_info;

    public ExceptionReturn(String exception_type, String exception_info) {
        this.exception_type = exception_type;
        this.exception_info = exception_info;
    }
    
    @Override
    public String toString() {
        return "ExceptionReturn: " + "exception_type = " + exception_type + " exception_info = " + exception_info;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof ExceptionReturn)) return false;
        ExceptionReturn exceptionReturn = (ExceptionReturn) obj;
        return this.exception_type.equals(exceptionReturn.exception_type) && this.exception_info.equals(exceptionReturn.exception_info);
    }
}
