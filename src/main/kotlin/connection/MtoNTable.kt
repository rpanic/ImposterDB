package connection

import java.util.*

class MtoNEntry<M : Observable, N : Observable>(val m: M, val n: N)