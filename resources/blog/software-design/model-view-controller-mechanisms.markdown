{:title "Model View Controller Mechanisms"
 :date "2009-06-07"
 :disqus-id "16 http://tomdalling.com/?p=16"
 :category "Software Design"}

The previous article, [Model View Controller Explained][previous_article],
explained what MVC is and why it's such a good design pattern.  The model view
controller design pattern has a flow. Actions flow from the view to the model
via certain pathways. Conversely, changes to the model flow through to the
view. This article will dive into finer detail about these pathways, and
discuss some of the specific mechanisms and design patterns used to implement
the flows.

<!--more-->

Observing The Model For Changes
-------------------------------

The model will change for a number of reasons. It will change in response to
user events. It can also change based on internal events such as timers, data
coming in over the network, etc. When the model changes, the view needs to be
notified so it can display the new information. Watching for changes in an
object is a common task, and is popularly known as "observation". There are
numerous design patterns that deal with observation; all are slightly
different, but have the same general purpose.

One such pattern is the "gang of four" [Observer Pattern][observer_pattern].
Here is an example of the pattern in Java:

```java
// Example 1

interface Observer {
    void observeChange(Object changedObject);
}

interface Observable {
    void addObserver(Observer o);
    void removeObserver(Observer o);
}

class PersonModel implements Observable {
    // ... (code omitted)
    public void setName(String name){
        _name = name;
        for(Observer o : _observers){
            o.observeChange(this);
        }
    }
}

class PersonView implements Observer {
    // ... (code omitted)
    public void setModel(PersonModel pm){
        _model.removeObserver(self);
        _model = pm;
        _model.addObserver(self);
    }

    public void observeChange(Object changedObject){
        if(changedObject == _model){
            this.updateView();
        }
    }
}
```

In the above example, the view knows when the model has changed because it adds
itself as an observer. J2SE actually provides an [Observer interface][observer_javadoc]
and an [Observable class][observable_javadoc] that perform the same function
(but differ to the example). For brevity, the above example doesn't contain a
controller, but the controller could be the `Observer` object.

One drawback is that it is difficult to tell what has actually changed in the
model. Maybe only a tiny part of the view needs to be redrawn, but because view
doesn't know that, it has to redraw everything which may be an expensive
operation.

Listening For Change Notifications From The Model
-------------------------------------------------

Another method of observation is through notifications. Notifications are very
similar to the observer pattern, but are more flexible. I really like the way
notifications are handled in Cocoa, and the [Cocoa documentation on
notifications][notifications_cocoadoc] explains notifications well.

Basically, the model object broadcasts different types of `Notification` objects,
possibly to a `NotificationCenter` object. Conversely, views/controllers register
to receive specific types of `Notification` objects. The model doesn't know or
care about who is listening, so it doesn't have to manage a list of observers.
Also, notifications can be specific enough to avoid the *"redraw everything
whenever the tiniest change happens"* problem with the observer pattern.

The `NotificationCenter` can also do cool things like coalescing. Coalescing
can be used to stop floods of update messages. For example, the model might be
changed 5000 times in a fraction of a second. Instead of redrawing the view
5000 times, the `NotificationCenter` can coalesce all the *"update the
view"* notifications into a single notification, meaning that the view is
redrawn just once at the end.

Catching User Actions With The Signal/Slot Design Pattern
---------------------------------------------------------

Subclassing every button and overriding `onClick()` is a tedious and
unnecessary way to intercept user actions. One better (but still nasty)
alternative is to tag every button with a string or a number, and have a big
handling function like so:

```cpp
// Example 2
void SomeWindow::handleClick(int buttonId){
    switch(buttonId){
        case OK_BUTTON: okClicked(); break;
        case CANCEL_BUTTON: cancelClicked(); break;
        //etc.
    }
}
```

What we're are aiming for is something that performs the logic *"when button X
is clicked, call function Y"*, and that is what the signal/slot design pattern
does.

View classes emit certain signals (e.g. *"button was clicked"*, *"finished
editing text"*). The controller class has certain slots (e.g. *"delete
contact"*, *"set contact name"*, etc). Any signal can be hooked up to any
compatible slot at run time. Signals can send arguments to slots, and a
"compatible" slot is a slot that can handle the arguments. For example the
"finished editing text" signal may send a single string argument, and the
*"set contact name"* slot accepts a single string argument, so they are
compatible.

The signal/slot pattern is easier to implement in languages with [dynamic
typing][dynamic_typing_wiki] and [first-class functions][first_class_fn_wiki],
but can still be done in languages such as C++. Examples of signal/slot
implementations can be seen in [Cocoa's target/action
mechanism][target_action_cocoadoc] (Objective-C),
[Boost.Signals][boot_signals_doc] (C++), and [Qt Signals and
Slots][qt_signals_doc] (C++ and other languages).

Catching User Actions With Delegation
-------------------------------------

Delegation is a pattern where an object sets itself as a "delegate" of a second
object. The second object calls functions on the delegate to inform it when
events occur, or ask it for information. Here is an example of a possible table
view delegate in Objective-C:

```objc
// Example 3
@implementation TDTableView
// ... (code omitted)
-(void)mouseDown:(NSPoint)point
{
    int rowClicked = [self rowAtPoint:point];
    if(_delegate && [_delegate respondsToSelector:@selector(tableView:rowWasClicked:)]){
        [_delegate tableView:self rowWasClicked:rowClicked];
    }
}
// ... (code omitted)
```

In the above example, the table view keeps an object `_delegate`. `_delegate` can
be any class of object, which reduces the dependency between the view and
whatever the delegate class is (probably the controller). When the view
receives a click, if the delegate is set and the delegate has a function called
`tableView:rowWasClicked:`, then that function is called on the delegate
object.

Here is a way you may implement this in C++:

```cpp
// Example 4
class TableViewDelegate {
    public:
        virtual void tableViewRowWasClicked(TableView tableView, int rowClicked);
        virtual void tableViewColumnWasClicked(TableView tableView, int columnClicked);
        //... (more delegate functions here)
};

class TableView {
    private:
        TableViewDelegate* _delegate;
    public:
        void setDelegate(TableViewDelegate* delegate);
        // ... (code omitted)
};

void TableView::mouseDown(Point p) {
    int rowClicked = rowAtPoint(p);
    if(_delegate){
        _delegate->tableViewRowWasClicked(this, rowClicked);
    }
}
```

The above example uses a technique called [dependency
injection][dependency_injection_wiki] to decouple the TableView from it's
delegate. In the MVC pattern, the delegate would be the controller object, and
would therefor inherit from `TableViewDelegate`.

Conclusion
----------

This article has discussed four common mechanisms used in MVC: the observer
pattern, notifications, the signal/slot design pattern, and the delegation
design pattern. Observation and notifications are useful for propagating
changes from the model to the view, and signals/slots and delegation are used
by the view to trigger changes in the model through the controller.

[previous_article]: /software-design/model-view-controller-explained "Model View Controller Explained"
[observer_pattern]: http://en.wikipedia.org/wiki/Observer_pattern "Observer Pattern"
[observer_javadoc]: http://java.sun.com/javase/6/docs/api/java/util/Observer.html "Observer Javadoc"
[observable_javadoc]: http://java.sun.com/javase/6/docs/api/java/util/Observable.html "Observable Javadoc"
[notifications_cocoadoc]: http://developer.apple.com/documentation/Cocoa/Conceptual/Notifications/Introduction/introNotifications.html
[dynamic_typing_wiki]: http://en.wikipedia.org/wiki/Dynamic_typing "Dynamic Typing"
[first_class_fn_wiki]: http://en.wikipedia.org/wiki/First-class_function "First-class function"
[target_action_cocoadoc]: http://developer.apple.com/documentation/Cocoa/Conceptual/CocoaFundamentals/CommunicatingWithObjects/CommunicateWithObjects.html#//apple_ref/doc/uid/TP40002974-CH7-SW14
[boot_signals_doc]: http://www.boost.org/doc/html/signals.html "Boost.Signals"
[qt_signals_doc]: http://doc.trolltech.com/signalsandslots.html
[dependency_injection_wiki]: http://en.wikipedia.org/wiki/Dependency_injection "Dependency injection"

