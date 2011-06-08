package org.semispace.semimeter.bean.mongo;


import java.io.Serializable;

public class PathElements implements Serializable {
    private String e1;
    private String e2;
    private String e3;
    private String e4;
    private String e5;

    public String getE1() {
        return e1;
    }

    public void setE1(final String e1) {
        this.e1 = e1;
    }

    public String getE2() {
        return e2;
    }

    public void setE2(final String e2) {
        this.e2 = e2;
    }

    public String getE3() {
        return e3;
    }

    public void setE3(final String e3) {
        this.e3 = e3;
    }

    public String getE4() {
        return e4;
    }

    public void setE4(final String e4) {
        this.e4 = e4;
    }

    public String getE5() {
        return e5;
    }

    public void setE5(final String e5) {
        this.e5 = e5;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("PathElements");
        sb.append("{e1='").append(e1).append('\'');
        sb.append(", e2='").append(e2).append('\'');
        sb.append(", e3='").append(e3).append('\'');
        sb.append(", e4='").append(e4).append('\'');
        sb.append(", e5='").append(e5).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
