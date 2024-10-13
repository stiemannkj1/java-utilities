package foo;

public final class Bar {
    public String toString() {
        return dev.stiemannkj1.bytecode.namespacer.Namespacer.class.getTypeName();
    }

    public static void main(final String[] args) {
        System.out.println(new Bar().toString());
    }
}