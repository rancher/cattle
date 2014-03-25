#!/bin/bash
set -e

URL=http://localhost:8080/v1

# Register yourself as an event handler
# We set the uuid so that the registration will only happen once.
# The second POST with the same UUID will result in a conflict.
curl -X POST ${URL}/externalhandlers -F uuid=demohandler1 -F name=demo -F processNames=instance.start

# Subscribe to event instance.start;handler=demo
#
# -N is important, that disables buffering
curl -s -N -X POST ${URL}/subscribe -F eventNames='instance.start;handler=demo' | while read EVENT; do 

    # Only reply to instance.start;handler=demo event and ignore ping event
    if [ "$(echo $EVENT | jq -r .name)" != "instance.start;handler=demo" ]; then
        continue
    fi

    # Just echo some stuff so we know whats going on
    echo $EVENT | jq .
    echo $EVENT | jq '"Starting \(.resourceType):\(.resourceId)"'

    # Example calling back to API to get other stuff...
    # This just constructs the command 
    #    curl -s http://localhost:8080/v1/container/42 | jq .
    #
    curl -s $(echo $EVENT | jq -r '"'$URL'/\(.resourceType)/\(.resourceId)"') | jq .

    # This would be the point you do something...
    echo "I am now doing really important other things..."

    # Reply
    # The required fields are 
    #    name: Should be equal to the replyTo of the request event
    #    previousId: Should be equal to the id of the request event
    #
    # In the data field you can put any data you want that will be set
    # on the instance.  The +data syntax means merge the existing value
    # with the one supplied.  So if {data: {a: 1 }} already exists on the
    # instance, and you speciy {+data: {b: 2}} then the result is
    # {data: {a: 1, b: 2}} and not {data: {b: 2}}
    if true; then
        echo "And now I'm done, so I'm going to say that"
        echo $EVENT | jq '{
            "name" : .replyTo,
            "previousIds" : [ .id ],
            "data" : {
                "+data" : {
                    "hello" : "world"
                }
            },
        }' | curl -s -X POST -H 'Content-Type: application/json' -d @- $URL/publish
    else
        echo "Crap, I failed.  I should tell somebody that"
        echo $EVENT | jq '{
            "name" : .replyTo,
            "previousIds" : [ .id ],
            "transitioning" : "error",
            "transitioningInternalMessage" : "Holy crap, stuff is really broken",
            "data" : {
                "+data" : {
                    "hello" : "world"
                }
            },
        }' | curl -s -X POST -H 'Content-Type: application/json' -d @- $URL/publish
    fi
done
