package edu.gemini.aspen.gds.api

import java.io.{IOException, File, FileInputStream, FileOutputStream}

/**
 * Set of common utility methods
 */
object Predef {
    /**
     * Copies a file from a given source to a destination
     *
     * @param from The source file, it cannot be null and the file should exist
     * @param to The destination file, it cannot be null
     */
    @throws(classOf[IOException])
    def copy(from: File, to: File) {
        require(from != null)
        require(from.exists)

        require(to != null)

        use(new FileInputStream(from)) {
            in => use(new FileOutputStream(to)) {
                out => out.getChannel.transferFrom(in.getChannel, 0, Long.MaxValue)
            }
        }
    }

    /**
     * Utility method to wrap a method applied in an object that has a close method.
     * The close method is closed after the block is called
     */
    def use[T <: {def close() : Unit}](closable: T)(block: T => Unit) {
        try {
            block(closable)
        } finally {
            closable.close()
        }
    }
}
