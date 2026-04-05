package rf.ebanina.utils.network;

public class Response {
    private int code;
    private StringBuilder body;

    public int getCode() {
        return code;
    }

    public Response setCode(int code) {
        this.code = code;
        return this;
    }

    public StringBuilder getBody() {
        return body;
    }

    public Response setBody(StringBuilder body) {
        this.body = body;
        return this;
    }

    @Override
    public String toString() {
        return body.toString();
    }
}