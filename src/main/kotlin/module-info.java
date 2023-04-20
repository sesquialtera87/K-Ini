module org.mth.kini {
    requires kotlin.stdlib;
    requires org.parboiled.core;
    requires org.parboiled.java;

    opens org.mth.kini to org.parboiled.java;
}