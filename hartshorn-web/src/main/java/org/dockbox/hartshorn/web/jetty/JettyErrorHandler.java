/*
 * Copyright (C) 2020 Guus Lieben
 *
 * This framework is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see {@literal<http://www.gnu.org/licenses/>}.
 */

package org.dockbox.hartshorn.web.jetty;

import org.dockbox.hartshorn.config.annotations.Value;
import org.dockbox.hartshorn.core.annotations.component.Component;
import org.dockbox.hartshorn.core.boot.Hartshorn;
import org.dockbox.hartshorn.core.context.ApplicationContext;
import org.dockbox.hartshorn.core.exceptions.ApplicationException;
import org.dockbox.hartshorn.web.RequestError;
import org.dockbox.hartshorn.web.RequestErrorImpl;
import org.dockbox.hartshorn.web.servlet.ErrorServlet;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.http.MimeTypes.Type;
import org.eclipse.jetty.io.ByteBufferOutputStream;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.util.StringUtil;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class JettyErrorHandler extends ErrorHandler {

    @Inject
    private ApplicationContext context;
    @Inject
    private ErrorServlet errorServlet;

    @Value(value = "hartshorn.web.headers.hartshorn")
    private boolean addHeader = true;

    @Override
    protected void generateAcceptableResponse(final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response, final int code, String message, final String contentType)
            throws IOException {
        if (message == null) message = HttpStatus.getMessage(code);

        try {
            final Charset charset = this.charSet(baseRequest, contentType);
            final ByteBuffer buffer = baseRequest.getResponse().getHttpOutput().getBuffer();
            final ByteBufferOutputStream out = new ByteBufferOutputStream(buffer);
            final PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, charset));

            response.setCharacterEncoding(charset.name());
            if (this.addHeader) response.addHeader("Hartshorn-Version", Hartshorn.VERSION);

            final Throwable th = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
            final RequestError error = new RequestErrorImpl(this.context, request, response, code, writer, message, th);

            this.errorServlet.handle(error);
            message = error.message();

            if (error.yieldDefaults()) {
                this.writeDefaults(request, response, writer, code, message);
            }
            writer.flush();
        }
        catch (final ApplicationException e) {
            this.context.handle("Could not handle request error", e);
            throw new IOException("Server error");
        }

        baseRequest.getHttpChannel().sendResponseAndComplete();
    }

    protected void writeDefaults(final HttpServletRequest request, final HttpServletResponse response, final PrintWriter writer, final int code, final String message) {
        response.setContentType(Type.TEXT_PLAIN.asString());
        writer.write("HTTP ERROR ");
        writer.write(Integer.toString(code));
        writer.write(' ');
        writer.write(StringUtil.sanitizeXmlString(message));
        writer.write("\n");
        writer.printf("URI: %s%n", request.getRequestURI());
        writer.printf("STATUS: %s%n", code);
        writer.printf("MESSAGE: %s%n", message);
    }

    protected Charset charSet(final Request baseRequest, final String contentType) {
        Charset charset = null;
        final List<String> acceptable = baseRequest.getHttpFields().getQualityCSV(HttpHeader.ACCEPT_CHARSET);
        if (!acceptable.isEmpty()) {
            for (final String name : acceptable) {
                if ("*".equals(name)) {
                    charset = StandardCharsets.UTF_8;
                    break;
                }

                try {
                    charset = Charset.forName(name);
                }
                catch (final Exception e) {
                    this.context.handle(e);
                }
            }
            if (charset == null)
                return StandardCharsets.UTF_8;
        }

        final MimeTypes.Type type;
        switch (contentType) {
            case "text/html", "text/*", "*/*", "text/plain" -> {
                if (charset == null)
                    charset = StandardCharsets.ISO_8859_1;
            }
            case "text/json", "application/json" -> {
                if (charset == null)
                    charset = StandardCharsets.UTF_8;
            }
            default -> charset = StandardCharsets.UTF_8;
        }
        return charset;
    }
}
