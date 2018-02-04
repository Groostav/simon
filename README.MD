Geoff's Fork of Simon for finding bugs, implementing new features

Some notes:

- Implementation of the `CompletableFuture` work was pretty straightforward, though tests were written in kotlin. See `EmpowerTests`.
- Performance has likely been slowed down here though this is speculation. I think i can offset some of the performance analysis of the deep(er)-message-inspection by simply replacing the many instances of `HashMap<Int, X>` with something more like `IntHashMap<TValue>`, (eg Trove's `TIntObjectHashMap` --though its license is a problem).
- I havent been considering binary compatibility with previous simon versions, though I should have no trouble keeping source compatibility. 

Bugs:

description | status
----------- | ------
`de.root1.simon.Dispatcher.generateSequenceId`'s treatment of atomic-int wrap-around logic was not thread safe. I implemented my (second ever?) CAS-loop to correct it. | SOLVED
proxy'd objects that return exceptions have those exceptions thrown by simon. My suggested solution is to follow the MsgAysnc, where we simply keep an extra field on the mssage for "thrown exception". | OPEN
 
